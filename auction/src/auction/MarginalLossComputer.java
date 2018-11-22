package auction;

//the list of imports
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import logist.agent.Agent;
import logist.task.Task;
import logist.topology.Topology.City;


public class MarginalLossComputer {

    enum Algo { TAKERANDOMWITHP, SIMULATEDANNEALING }
    
    private City[] homeCity;
    private int[] capacity;
    private int[] costPerKm;
    
    private City[] oppHomeCity;
    private int[] oppCapacity;
    private int[] oppCostPerKm;
    
    private Algo algorithm;
    private double parameter1;
    private double parameter2;
    
    public MarginalLossComputer(Agent agent) {
        
    	homeCity = new City[agent.vehicles().size()];
    	capacity = new int[homeCity.length];
    	costPerKm = new int[homeCity.length];
    	
    	for (int i=0; i < homeCity.length; i++) {
    		homeCity[i] = agent.vehicles().get(i).homeCity();
    		capacity[i] = agent.vehicles().get(i).capacity();
    		costPerKm[i] = agent.vehicles().get(i).costPerKm();
    	}
    			
        algorithm = Algo.valueOf(agent.readProperty("algorithm", String.class, "SIMULATEDANNEALING").toUpperCase());
        switch (algorithm) {
        case TAKERANDOMWITHP: 
        	parameter1 = agent.readProperty("probability", Double.class, 0.95);
        	break;
        case SIMULATEDANNEALING:
        	parameter1 = agent.readProperty("temperature-begin", Double.class, 1000.);
        	parameter2 = agent.readProperty("temperature-end", Double.class, 100.);
        	break;
        }
    }
    
    /*
     * updates the homeCity, capacity and costPerKm of the opponent
     */
    public void updateOpponent(ArrayList<Integer> oppStartCities, int maxVehicleCapacity, int avgCostPerKm, List<City> citites) {
    	oppHomeCity = new City[oppStartCities.size()];
    	oppCapacity = new int[oppHomeCity.length];
    	oppCostPerKm = new int[oppHomeCity.length];
    	
    	for (int i = 0; i < oppHomeCity.length; i++) {
    		oppHomeCity[i] = citites.get(oppStartCities.get(i));
    		oppCapacity[i] = maxVehicleCapacity;
    		oppCostPerKm[i] = avgCostPerKm;
    	}
    }

    /*
     * return the best solution (found by stochastic search) to the problem of delivering *tasks* Tasks (assuming ot be non empty)
     * with *vehicles* vehicles before the given *timeLimit*, if *forOurAgent* is set to true, we compute a solution for the main 
     * agent oterwise we do it for the opponent
     */
    public Solution getSolution(ArrayList<Task> tasks, long timeLimit, boolean forOurAgent) {
    	int[] tasksWeights = new int[tasks.size()];
    	City[] TaksPickupCity = new City[tasks.size()];
    	City[] TaskDeliverCity = new City[tasks.size()];
    	
    	for (int i=0; i < tasks.size(); i++) {
    		Task task = tasks.get(i);
    		tasksWeights[i] = task.weight;
    		TaksPickupCity[i] = task.pickupCity;
    		TaskDeliverCity[i] = task.deliveryCity;
    	}
    	
    	return getSolution(tasksWeights, TaksPickupCity, TaskDeliverCity, timeLimit, forOurAgent);
    }
    
    /*
     * same function as above but works with non tasks arguments so because we can't create tasks trough the logist API
     */
    public Solution getSolution(int[] tasksWeights, City[] TaksPickupCity, City[] TaskDeliverCity, long timeLimit, boolean forOurAgent) {
    	Solution solution;
    	if (forOurAgent) {
    		solution = new Solution(tasksWeights, TaksPickupCity, TaskDeliverCity, homeCity, capacity, costPerKm);
    	} else {
    		solution = new Solution(tasksWeights, TaksPickupCity, TaskDeliverCity, oppHomeCity, oppCapacity, oppCostPerKm);
    	}
        
        if (tasksWeights.length == 0) { // no search to perfom
        	return solution;
        }
        
        switch (algorithm) {
        case TAKERANDOMWITHP:
        	return stochasticSearchTakeRandomWithP(solution, parameter1, timeLimit);
        case SIMULATEDANNEALING:
        	return simulatedAnnealing(solution, parameter1, parameter2, timeLimit);
        default:
        	return null;
        }
    }
    
    /* performs the stochastic search, at each stage, with probability p, we take the neighbourgh
     * with best score, with probability 1-p, we take a random new neighbourg
     */
	private Solution stochasticSearchTakeRandomWithP(Solution solution, double probability, long timeLimit) {
		Solution bestCurrentSolution = solution;
		Random generator = new Random();
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
		Random generator = new Random();
		Solution newSol, bestCurrentSolution = solution, bestNeighborhoodSol;
		double temperature = temperatureInit, diffScore;
		long startTime = System.currentTimeMillis(), deltaTime = timeLimit - startTime;
		ArrayList<Solution> neighbourgs;
				
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
}
