package auction;

//the list of imports
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.task.Task;


public class MarginalLossComputer {

    enum Algo { TAKERANDOMWITHP, SIMULATEDANNEALING }
    
    private List<Vehicle> vehicles;
    private Algo algorithm;
    private double parameter1;
    private double parameter2;
    
    public MarginalLossComputer(Agent agent) {
        
    	vehicles = agent.vehicles();
    			
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
     * return the best solution (found by stochastic search) to the problem of delivering *tasks* Tasks (assuming ot be non empty)
     * with *vehicles* vehicles before the given *timeLimit*
     */
    public Solution getSolution(ArrayList<Task> tasks, long timeLimit) {
        long SEED = 17;
        Solution solution = new Solution(tasks, this.vehicles, SEED);
        
        if (tasks.size() == 0) { // no search to perfom
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
		long startTime = System.currentTimeMillis();
		
		// loop body os as to evaluate the time of an iteration
		ArrayList<Solution> neighbourgs = solution.generateNeighbours();
		if (neighbourgs.size() > 0) {
			if (generator.nextDouble() < probability) {
	    		solution = Collections.min(neighbourgs);
	    	} else {
	    		solution = neighbourgs.get(generator.nextInt(neighbourgs.size()));
	    	}
	    	if (solution.cost < bestCurrentSolution.cost) {
	    		bestCurrentSolution = solution;
	    	}
		}
    	
    	timeLimit -= 3*(System.currentTimeMillis() - startTime);  // adding a safety margin of 3 iterations
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
		
		timeLimit -= 3*(System.currentTimeMillis() - startTime);  // adding a safety margin of 3 iterations
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
}
