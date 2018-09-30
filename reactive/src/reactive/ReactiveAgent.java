package reactive;

import java.util.Arrays;
import java.util.Random;

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

public class ReactiveAgent implements ReactiveBehavior {

	private Random random;
	private Agent myAgent;
	private int numActions;
	private BiHashMap<String, Integer> stateMap;
	private double[][][] QTable;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);

		this.random = new Random();
		this.myAgent = agent;
		numActions = 0;
		stateMap = new BiHashMap<String, Integer>();
		int stateId = 0;
		for (City city : topology) {
			stateMap.put(getStateString(city.id, -1), stateId++); // we have no task
			for (City dest : topology) {
				if (td.probability(city, dest) != 0.0) {
					stateMap.put(getStateString(city.id, dest.id), stateId++); // we have a task for city dest
				}
			}
		}
		
		QTable = new double[agent.vehicles().size()][stateId][topology.size() + 1]; // every cell is implicitely set to 0.0
		// actions: [take package, go to city 0, go to city 1, ...]
		double[] stateValues = new double[stateId];
		
		// Value Iteration
		for (int i = 0; i < agent.vehicles().size(); i++) {
			Arrays.fill(stateValues, 0.0);  // reset the states Values to 0
			Vehicle vehicule = agent.vehicles().get(i);
			for (double maxDiff = 0.0; maxDiff < 1e-7;) {
				for (int state = 0; state < QTable.length; state++) {
					int[] fromAndTo = getStateIds(stateMap.getKey(state));
					City from = (topology.cities()).get(fromAndTo[0]);
					double best = -Double.MAX_VALUE;
					if (fromAndTo[1] != -1.0) {
						City to = (topology.cities()).get(fromAndTo[1]);
						QTable[i][state][0] = td.reward(from, to) - from.distanceTo(to)*vehicule.costPerKm()
								+ discount*getFutureReward(to, topology, td, stateValues);
						best = Math.max(best, QTable[i][state][0]);
					}
					for(City neighbourg : from) {
						QTable[i][state][neighbourg.id + 1] = -from.distanceTo(neighbourg)*vehicule.costPerKm()
								+ discount*getFutureReward(neighbourg, topology, td, stateValues);
						best = Math.max(best, QTable[i][state][neighbourg.id + 1]);
					}
					maxDiff = Math.max(maxDiff, Math.abs(stateValues[state] - best));
					stateValues[state] = best;
				}
			}
		}
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		int dest_id;
		
		if (availableTask == null) {
			dest_id = -1;
		} else {
			dest_id = availableTask.deliveryCity.id;
		}
		int state_id = stateMap.get(getStateString(vehicle.getCurrentCity().id, dest_id));
		
		int maxOffset = -1;
		double max = -Double.MAX_VALUE;
		if (availableTask != null) {
			maxOffset = 0;
			max = QTable[vehicle.id()][state_id][0];
		}
		
		City bestNeighbourg = null;
		for (City neighbourg : vehicle.getCurrentCity()) {
			double QTableValue = QTable[vehicle.id()][state_id][neighbourg.id + 1];
			if (QTableValue > max) {
				maxOffset = neighbourg.id + 1;
				max = QTableValue;
				bestNeighbourg = neighbourg;
			}
		}
		
		// maxOffset != -1 because a city has to have neighbourgs
		if (maxOffset == 0) {
			action = new Pickup(availableTask);
		} else {
			action = new Move(bestNeighbourg);
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after " + numActions + " actions is " + myAgent.getTotalProfit()
			 + " (average profit: " + (myAgent.getTotalProfit() / (double)numActions) + ")");
		}
		numActions++;
		
		return action;
	}
	
	// get a String Id for the state from the id of the base city and the id of the destination,
	// -1 is used to represent taking a task
	private String getStateString(int city_id, int dest_id) {
		return Integer.toString(city_id) + "_" + Integer.toString(dest_id);
	}
	
	// performs the inverse of getStateString, get the Id of the state and the Id of the dest from the String
	private int[] getStateIds(String stateString) {
		int[] ids = new int[2];
		String[] str = stateString.split("_");
		ids[0] = Integer.parseInt(str[0]);
		ids[1] = Integer.parseInt(str[1]);
		return ids;
	}
	
	// computes the future reward associated with going to a city
	double getFutureReward(City city, Topology topology, TaskDistribution td, double[] stateValues) {
		double reward = 0.0, probability = 0.0, cum_proba = 0.0;
		for(City to: topology) {
			probability = td.probability(city, to);
			if (probability != 0.0) {
				cum_proba += probability;
				reward += probability*stateValues[stateMap.get(getStateString(city.id, to.id))];
			}
		}
		reward += (1.0 - cum_proba)*stateValues[stateMap.get(getStateString(city.id, -1))];
		return reward;
	}
}
