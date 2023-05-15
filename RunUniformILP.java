package com.google.ortools;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import java.util.*;


public class RunUniformILP {
    
    public static void main(String args[]) {
        String data_set = "cor_allsports";
        String delimiter = "\\s"; 
        ArrayList<ArrayList<Integer>> prob_matrix = Helper.read_large_network_relabel("Data/"+ data_set + "/graph.txt", delimiter);
        int num_nodes = prob_matrix.size();
        System.out.println("Data set with " + num_nodes + " nodes");

        Loader.loadNativeLibraries();

        // STANDARD TEST

        int ROUNDS = 10;

        int[] k_vals = {3, 6}; //, 15, 20, 25, 30, 35, 40, 45, 50};

        for (int q = 0; q < k_vals.length; q++) {

        int K = k_vals[q];
        System.out.println("Start: k = " + K);

        long lpStart = System.currentTimeMillis();

        MPSolver solver = MPSolver.createSolver("SCIP");

        MPVariable[][] var_array = new MPVariable[num_nodes][num_nodes];

        for (int i = 0; i < num_nodes; i++) {
            for (int j = i+1; j < num_nodes; j++) {
                // String name = i + "_" + j;
                var_array[i][j] = solver.makeIntVar(0.0, 1.0, i + "_" + j); // + if i < j
                var_array[j][i] = solver.makeIntVar(0.0, 1.0, j + "_" + i); // - if i > j
            }
        }
    
        System.out.println("Number of variables = " + solver.numVariables());

        // probability constraints
        for (int i = 0; i < num_nodes; i++) {
            for (int j = i + 1; j < num_nodes; j++) {
            MPConstraint ct = solver.makeConstraint(1.0, 1.0, i + "_" + j);
            ct.setCoefficient(var_array[i][j], 1); // assume all other variables are 0?
            ct.setCoefficient(var_array[j][i], 1);
            }
        }

        // size constraints
        for (int i = 0; i < num_nodes; i++) {
            MPConstraint ct = solver.makeConstraint(0.0, K - 1, i + ""); // reduce K by 1 for PM notation
            for (int j = 0; j < num_nodes; j++) {
                // count number of nodes in same cluster with i
                if (i < j)
                    ct.setCoefficient(var_array[i][j], 1); // assume all other variables are 0?
                else if (i > j)
                    ct.setCoefficient(var_array[j][i], 1);
            }
        }

        System.out.println("Constraints so far = " + solver.numConstraints());


        // triangle inequality constraints
        for (int i = 0; i < num_nodes; i++) {
            for (int j = i + 1; j < num_nodes; j++) {
                for (int k = j + 1; k < num_nodes; k++) {
                    MPConstraint ct = solver.makeConstraint(0.0, 2.0, i + "_" + j + "_" + k);
                    ct.setCoefficient(var_array[j][i], 1); // assume all other variables are 0?
                    ct.setCoefficient(var_array[k][j], 1);     
                    ct.setCoefficient(var_array[k][i], -1); 
                }
    
            }
        }

        System.out.println("Number of constraints = " + solver.numConstraints());

        MPObjective objective = solver.objective();
        for (int i = 0; i < num_nodes; i++) {
            for (int j = i + 1; j < num_nodes; j++) {
                int prob = 0;
                if (prob_matrix.get(i).contains(j))
                    prob = 1; // edge exists!
                objective.setCoefficient(var_array[i][j], 1 - prob);
                objective.setCoefficient(var_array[j][i], prob);
            }
        }

        objective.setMinimization();
        // solver.setTimeLimit(600000); // 10 minute limit
        solver.solve();
        long lpTime = System.currentTimeMillis() - lpStart;

        System.out.println("LP Objective value = " + objective.value());
        System.out.println("Total time: " + (lpTime / 1000.0));
        System.out.println();

   

        }



    }

    



}
