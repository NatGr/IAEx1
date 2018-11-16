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

    enum Algo { TAKERANDOMWITHP, SIMULATEDANNEALING, STOCHASTICRESTART}
    
    private Topology topology;
    private TaskDistribution distribution;
    private ArrayList<Task> tasks;
    private List<Vehicle> vehicles;
    private long timeout_setup;
    private long timeout_plan;
    private Algo algorithm;
    private double parameter1;
    private double parameter2;
    
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
        	parameter1 = agent.readProperty("probability", Double.class, 0.95);
        	break;
        case SIMULATEDANNEALING:
        	parameter1 = agent.readProperty("temperature-begin", Double.class, 1000.);
        	parameter2 = agent.readProperty("temperature-end", Double.class, 100.);
        	break;
        case STOCHASTICRESTART:
        	parameter1 = agent.readProperty("probability", Double.class, 0.95);
        	parameter2 = agent.readProperty("threshold", Double.class, 30.);
        }
        
        this.topology = topology;
        this.distribution = distribution;
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_limit = System.currentTimeMillis() + (long) (0.999*timeout_plan) - 25;  
        // we use a 0.999 factor margin for safety and a 20 ms safety margin for computing
        // the plan
        
        ArrayList<Task> list = new ArrayList<Task>();
        for (Task task: tasks) {
        	list.add(task);
        }
        this.tasks = list;
        this.vehicles = vehicles;
        
        Solution solution = new Solution(list, vehicles);
        if (list.size() == 0) {  // if we call generateNeighbours for a task with no solutions
        	// we will loop infinitely
        	return solution.getPlans(list);
        }
        
        switch (algorithm) {
        case TAKERANDOMWITHP:
        	solution = stochasticSearchTakeRandomWithP(solution, parameter1, time_limit);
        	break;
        case SIMULATEDANNEALING:
        	solution = simulatedAnnealing(solution, parameter1, parameter2, time_limit);
        	break;
        case STOCHASTICRESTART:
        	solution = stochasticSearchRestart(solution, parameter1, (int) parameter2, time_limit);
        	break;
        }
        
		System.out.println(solution.cost);
        List<Plan> plans = solution.getPlans(list);
        return plans;
    }
    
    /* stochastic search with restarts: performs stochastic search
     * and restarts once the best score doesn't change anymore
     */
    private Solution stochasticSearchRestart(Solution solution, double probability, int iterThreshold, long timeLimit) {
		Solution bestCurrentSolution = solution;
		Random generator = new Random();
		long startTime = System.currentTimeMillis();
		int haventMadeProgressSince = 0, restartPointsMaxSize = 5000;
		ArrayList<Solution> neighbourgs, restartPoints = new ArrayList<Solution>(restartPointsMaxSize);
		
		
        while (System.currentTimeMillis() < timeLimit) {
        	neighbourgs = solution.generateNeighbours();
        	if (neighbourgs.size() == 0) {  // possible if very restrictive constraints
				continue;
			}
        	
        	if (generator.nextDouble() < probability) {
        		solution = Collections.min(neighbourgs);
        	} else {
        		solution = neighbourgs.get(generator.nextInt(neighbourgs.size()));
        	}
        	
        	if (solution.cost < bestCurrentSolution.cost) {
        		bestCurrentSolution = solution;
        		haventMadeProgressSince = 0;
        	} else {
        		haventMadeProgressSince++;
        		if (restartPoints.size() < restartPointsMaxSize && generator.nextDouble() < 0.05) {
        			restartPoints.add(solution);
        		}
        	}
        	
        	if (haventMadeProgressSince > iterThreshold) {
        		solution = restartPoints.get(generator.nextInt(restartPoints.size()));
        		haventMadeProgressSince = 0;
        	}
        }
        return bestCurrentSolution;
	}
    
    
    /* performs the stochastic search, at each stage, with probability p, we take the neighbourgh
     * with best score, with probability 1-p, we take a random new neighbourg
     */
	private Solution stochasticSearchTakeRandomWithP(Solution solution, double probability, long timeLimit) {
		Solution bestCurrentSolution = solution;
		Random generator = new Random();
		long startTime = System.currentTimeMillis();
		ArrayList<Solution> neighbourgs;
		
        while (System.currentTimeMillis() < timeLimit) {
        	neighbourgs = solution.generateNeighbours();
        	if (neighbourgs.size() == 0) {  // possible if very restrictive constraints
				continue;
			}
        	if (generator.nextDouble() < probability) {
        		solution = Collections.min(neighbourgs);
        	} else {
        		solution = neighbourgs.get(generator.nextInt(neighbourgs.size()));
        	}
        	if (solution.cost < bestCurrentSolution.cost) {
        		bestCurrentSolution = solution;
        	}   
        }
        return bestCurrentSolution;
	}

	/* performs a simmulated annealing, the temperature is decreased linearly at each iteration
	 * to be equal to temperatureInit on the first one and temperatureEnd on the last one (approximately)
	 * 
	 */
	private Solution simulatedAnnealing(Solution solution, double temperatureInit, 
			double temperatureEnd, long timeLimit) {
		int iterations_best = 0;
		Random generator = new Random();
		Solution newSol, bestCurrentSolution = solution, bestNeighborhoodSol;
		double temperature = temperatureInit, diffScore;
		long startTime = System.currentTimeMillis(), deltaTime;
		
		ArrayList<Solution> neighbourgs = solution.generateNeighbours();
		if (neighbourgs.size() > 0) {
			bestNeighborhoodSol = Collections.min(neighbourgs);
			if (bestNeighborhoodSol.cost < bestCurrentSolution.cost) {
	    		bestCurrentSolution = bestNeighborhoodSol;
	    	}
			newSol = neighbourgs.get(generator.nextInt(neighbourgs.size()));  // pick random solution
			diffScore = newSol.cost - solution.cost;
	        if (diffScore < 0) {
	            solution = newSol;
	        } else if (generator.nextDouble() < Math.exp(- diffScore / temperature)) { // accept with a probability that decreases with the temperature
	            // and that is smaller the worse the new solution is
	            solution = newSol;
	        }
		}
		
		deltaTime = timeLimit - startTime;
				
		for (double currentTime = System.currentTimeMillis(), fractionTimeLeft; currentTime < timeLimit; currentTime = System.currentTimeMillis()) {
			fractionTimeLeft = (timeLimit - currentTime) / deltaTime;
			temperature = temperatureInit * fractionTimeLeft + temperatureEnd * (1-fractionTimeLeft); // update the temperature 
					
			neighbourgs = solution.generateNeighbours();
			if (neighbourgs.size() == 0) {  // possible if very restrictive constraints
				continue;
			}
			bestNeighborhoodSol = Collections.min(neighbourgs);
			if (bestNeighborhoodSol.cost < bestCurrentSolution.cost) {
	    		bestCurrentSolution = bestNeighborhoodSol;
	    	}
			newSol = neighbourgs.get(generator.nextInt(neighbourgs.size())); // pick random solution
			
			diffScore = newSol.cost - solution.cost;
			if (diffScore <= 0) {
				solution = newSol;
			} else if (generator.nextDouble() < Math.exp(- diffScore / temperature)) { // accept with a probability that decreases with the temperature
				// and that is smaller the worse the new solution is
				solution = newSol;
			}
        }

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
