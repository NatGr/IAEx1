package reactive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javafx.util.Pair;
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveDummy implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private Agent myAgent;
	private int numActions;
	private BiHashMap<String, Integer> stateMap;
	private HashMap<City, Double> expected_rewards = new HashMap<City, Double>();
	private ArrayList<City> city_ranking = new ArrayList<City>();;
	private double[][][] QTable;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);
		//Calculate value to attach to each city based on the reward you can earn by leaving the city
		for (City c1:topology) {
			double reward=0;
			for(City c2:topology) {
				reward+=td.probability(c1, c2)*td.reward(c1, c2);	
			}
			expected_rewards.put(c1, reward);
		}
		
		/*Build a ranking of the cities by the average reward you will get in that city*/
		List<City> city_list = topology.cities();
		ArrayList<City> cities = new ArrayList<City>(city_list);
		while(!cities.isEmpty()) {
			City best_city = cities.get(0);
			for(City c:cities) {
				if(expected_rewards.get(c) > expected_rewards.get(best_city)) {
					best_city = c;
				}
			}
			city_ranking.add(best_city);
			cities.remove(best_city);
		}
		

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		
		City currentCity = vehicle.getCurrentCity();
		Action action = new Move(currentCity.randomNeighbor(random));	//Initialisation
		if (availableTask == null ) { 
			for (City c:city_ranking) {
				if (currentCity.hasNeighbor(c)) { //Go to best neighbour, unregarding the distance to it
					action = new Move(c);
					numActions++;
					return action;
				}
			}		
		} else {
			for (City c:city_ranking) {
				if (currentCity.hasNeighbor(c) && (expected_rewards.get(c))>availableTask.reward) { //See if there is a better neighbour but take into account the distance
					action = new Move(c);
					numActions++;
					return action;
				}
			}
			action = new Pickup(availableTask);
		}
		
		if (numActions >= 1) {
			System.out.println("Agent Dummmy: The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		return action;
	}

	
}
