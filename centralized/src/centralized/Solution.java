package centralized;

import java.util.List;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class Solution {
	// what we call Task in the following is either a task delivery or a task pickup
	private final int nbrVehicles, nbrTasks;
	private final int[] weight;  // the weight of every Task
	private final City[] city;  // the city corresponding to every Task and every Vehicle (initial city)
	// that array is orderer by : [Task_0 (pickup), Task_0 (delivery), ..., Task_n (delivery), Vehicle_m, ..., Vehicle_0]
	private final int[] vehicleCapacity;  // initial capacity of every vehicle
	private final int[] vehicleCostPerKm;  // cost per Km of every vehicle
	
	// solution characteristics:
	private int[] remainingCapacity;  // the remaining capacity of the vehicle when he accepts the corresponding Task
	private int[] vehicle;  // the offset of the vehicle corresponding the the Task
	private int[] time;  // the time offset of the given Task
	int[] nextTask; // offset of the next Task or -1 if the next Task is null
	// that array is orderer by : [Task_0 (pickup), Task_0 (delivery), ..., Task_n (delivery), Vehicle_m, ..., Vehicle_0]
	double score;  // score of the current solution
	
	/* initializes all problem variables and generates a first valid solution randomly
	 * we assume there are no tasks that no vehicle is able to carry (since that would make the 
	 * problem unsolvable
	 */
	public Solution(Task[] tasks, List<Vehicle> vehicles) {
		nbrVehicles = vehicles.size();
		nbrTasks = 2*tasks.length;
		weight = new int[nbrTasks];
		city = new City[nbrTasks + nbrVehicles];
		vehicleCapacity = new int[nbrVehicles];
		vehicleCostPerKm = new int[nbrVehicles];
		remainingCapacity = new int[nbrTasks];
		vehicle = new int[nbrTasks];
		time = new int[nbrTasks];
		nextTask = new int[nbrTasks + nbrVehicles];
		
		for (int i = 0, pickup = 0, deliver = 1; i < tasks.length; i++, pickup += 2, deliver += 2) {
			weight[pickup] = tasks[i].weight;
			weight[deliver] = - weight[pickup];  // delivering a task is equivalent to taking a task of negtaive capacity
			city[pickup] = tasks[i].pickupCity;
			city[deliver] = tasks[i].deliveryCity;
		}
		
		for (int i = 0; i < nbrVehicles; i++) {
			city[city.length - i] = vehicles.get(i).homeCity();
			vehicleCapacity[i] = vehicles.get(i).capacity();
			vehicleCostPerKm[i] = vehicles.get(i).costPerKm();
		}
		
		// starting solution:
		
		computeScore();
	}
	
	// computes the score of the solution
	private void computeScore() {
		score = 0;
		for (int i = 0, vehicleDrivenDistance = 0, offset; i < nbrVehicles; i++) {
			// iterates over all the tasks the vehicle performs
			offset = nextTask.length - i;
			for(int current = offset, next = nextTask[offset]; next != -1; current = next, next = nextTask[next]) {
				vehicleDrivenDistance += city[current].distanceTo(city[next]);
			}
			score += vehicleCostPerKm[i]*vehicleDrivenDistance;
		}
	}
	
	private boolean checkConstraints() {
		return true;
	}
}
