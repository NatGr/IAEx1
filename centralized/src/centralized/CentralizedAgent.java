package centralized;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import logist.LogistSettings;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class CentralizedAgent implements CentralizedBehavior {

    enum Algo { TAKERANDOMWITHP }
    
    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    private Algo algorithm;
    private double parameter;
    
    @Override
    public void setup(Topology topology, TaskDistribution distribution,
            Agent agent) {
        
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        
        algorithm = Algo.valueOf(agent.readProperty("algorithm", String.class, "TakeRandomWithP").toUpperCase());
        switch (algorithm) {
        case TAKERANDOMWITHP: 
        	parameter = agent.readProperty("probability", Double.class, 0.6);
        	break;
        }
        
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_limit = System.currentTimeMillis() + (long) (0.999*timeout_plan) - 20;  
        // we use a 0.999 factor margin for safety and a 20 ms safety margin for computing
        // the plan
        
        long SEED = 17;
        ArrayList<Task> list = new ArrayList<Task>();
        for (Task task: tasks) {
        	list.add(task);
        }
        Solution solution = new Solution(list, vehicles, SEED);
        if (list.size() == 0) {  // if we call generateNeighbours for a task with no solutions
        	// we will loop infinitely
        	return solution.getPlans(list);
        }
        
        // remembers the best
        // p: one of them, 1-p: another randomly
        // or same with p decaying
        // or if best_neighbourg > max take else simmulated annealing
        switch (algorithm) {
        case TAKERANDOMWITHP:
        	solution = stochasticSearchTakeRandomWithP(solution, parameter, time_limit);
        	break;
        }
        
        
        List<Plan> plans = solution.getPlans(list);
        return plans;
    }
    
    /* performs the stochastic search, at each stage, with probability p, we take the neighbourgh
     * with best score, with probability 1-p, we take a random new neighbourg
     */
	Solution stochasticSearchTakeRandomWithP(Solution solution, double probability, long time_limit) {
		Solution bestCurrentSolution = solution;
		Random generator = new Random();
		long time_start = System.currentTimeMillis();
		
		// loop body
		ArrayList<Solution> neighbourgs = solution.generateNeighbours();
    	if (generator.nextDouble() < probability) {
    		solution = Collections.min(neighbourgs);
    	} else {
    		solution = neighbourgs.get(generator.nextInt(neighbourgs.size()));
    	}
    	if (solution.cost < bestCurrentSolution.cost) {
    		bestCurrentSolution = solution;
    	}
    	
    	time_limit -= 3*(System.currentTimeMillis() - time_start);  // adding a safety margin of 3 iterations
        while (System.currentTimeMillis() < time_limit) {
        	neighbourgs = solution.generateNeighbours();
        	if (generator.nextDouble() < probability) {
        		solution = Collections.min(neighbourgs);
        	} else {
        		solution = neighbourgs.get(generator.nextInt(neighbourgs.size()));
        	}
        	if (solution.cost < bestCurrentSolution.cost) {
        		bestCurrentSolution = solution;
        	}        	
        }
        System.out.println(bestCurrentSolution.cost);
        return bestCurrentSolution;
	}

    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity)) {
                plan.appendMove(city);
            }

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (City city : task.path()) {
                plan.appendMove(city);
            }

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }
}
