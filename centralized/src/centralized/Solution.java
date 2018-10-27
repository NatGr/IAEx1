package centralized;

import java.util.Collections;
import java.util.List;
import java.util.Random;

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
	// that array is orderer by : [Task_0 (pickup), Task_0 (delivery), ..., Task_n (delivery), Vehicle_0, ..., Vehicle_m]
	private final int[] vehicleCapacity;  // initial capacity of every vehicle
	private final int[] vehicleCostPerKm;  // cost per Km of every vehicle
	
	// solution characteristics:
	private int[] remainingCapacity;  // the remaining capacity of the vehicle when he accepts the corresponding Task
	private int[] vehicle;  // the offset of the vehicle corresponding the the Task
	private int[] time;  // the time offset of the given Task
	int[] nextTask; // offset of the next Task or -1 if the next Task is null
	// that array is orderer by : [Task_0 (pickup), Task_0 (delivery), ..., Task_n (delivery), Vehicle_0, ..., Vehicle_m]
	double score;  // score of the current solution
	
	/* initializes all problem variables and generates a first valid solution
	 * if there are tasks that no vehicle is able to carry, the problem is unsolvable and we throw an IllegalArgumentException
	 */
	public Solution(List<Task> tasks, List<Vehicle> vehicles, long seed) throws IllegalArgumentException {
		nbrVehicles = vehicles.size();
		nbrTasks = 2*tasks.size();
		weight = new int[nbrTasks];
		city = new City[nbrTasks + nbrVehicles];
		vehicleCapacity = new int[nbrVehicles];
		vehicleCostPerKm = new int[nbrVehicles];
		remainingCapacity = new int[nbrTasks];
		vehicle = new int[nbrTasks];
		time = new int[nbrTasks];
		nextTask = new int[nbrTasks + nbrVehicles];
		
		int maxCapacity = Integer.MIN_VALUE;
		for (int i = 0; i < nbrVehicles; i++) {
			city[nbrTasks + i] = vehicles.get(i).homeCity();
			vehicleCapacity[i] = vehicles.get(i).capacity();
			maxCapacity = Math.max(maxCapacity, vehicleCapacity[i]);
			vehicleCostPerKm[i] = vehicles.get(i).costPerKm();
		}
		
		for (int i = 0, pickup = 0, deliver = 1; i < tasks.size(); i++, pickup += 2, deliver += 2) {
			weight[pickup] = tasks.get(i).weight;
			if (weight[pickup] > maxCapacity) {
				throw new IllegalArgumentException("One of the tasks has a weight that is too big for any vehicle, the problem is unsolvable.");
			}
			weight[deliver] = - weight[pickup];  // delivering a task is equivalent to taking a task of negtaive capacity
			city[pickup] = tasks.get(i).pickupCity;
			city[deliver] = tasks.get(i).deliveryCity;
		}
		
		// starting solution, we assign the tasks randomly but so as to respect the constraints:
		Random generator = new Random(seed);
		Collections.shuffle(tasks, generator);
		for (Task task: tasks) {
			for (int vehicle = generator.nextInt(nbrVehicles); ; vehicle = generator.nextInt(nbrVehicles)) {
				
			}
		}
		
		// compute our solution's score
		computeScore();
	}
	
	// computes the score of the solution
	private void computeScore() {
		score = 0;
		for (int i = nbrTasks, vehicleDrivenDistance = 0, offset; i < nbrVehicles; i++) {
			// iterates over all the tasks the vehicle performs
			offset = nbrTasks + i;
			for(int current = offset, next = nextTask[offset]; next != -1; current = next, next = nextTask[next]) {
				vehicleDrivenDistance += city[current].distanceTo(city[next]);
			}
			score += vehicleCostPerKm[i]*vehicleDrivenDistance;
		}
	}
	
	private boolean checkConstraints() {
		
		return true;
	}
	
	//nextTask(t)!=t
	private boolean checkNextEqualsSelf() {
		//i is index of task
		for (int i = 0; i < nbrTasks; i++) {
			if (nextTask[i] == i)
				return false;
		}
		return true;
	}
	
	//nextTask(v_k)=t_j => time(t_j)=1
	private boolean checkNextVehicleTime() {
		//iterate over all vehicles
		for (int i = nbrTasks; i < nextTask.length; i++) {
			//nextTask[i] is the first task of vehicle i-nbrTasks
			if(time[nextTask[i]] != 1) {
				if (nextTask[i] != -1) { //possibility that a vehicle doesn't execute a task at all
					return false;
				}
			}
		}
		return true;
	}
	
	//nextTask(t_i)=t_j => time(t_j)=time(t_i)+1
	private boolean checkTimeNextTask() {
		for (int i = 0; i < nbrTasks; i++) {
			if(nextTask[i] == -1) { //final task so don't need to check time of nexttask
				continue;
			}
			if(time[i] + 1 != time[nextTask[i]]) {
				return false;
			}
		}
		return true;
	}
	
	//nextTask(v_k)=t_j => vehicle(t_j)=v_k
	private boolean checkVehicleTaskPair() {
		for (int i = nbrTasks; i < nbrVehicles; i++) {
			if (vehicle[nextTask[i]] != i) {
				return false;
			}
		}		
		return true;
	}
	
	//nextTask(t_i)=t_j => vehicle(t_j) = vehicle(t_i)
	private boolean checkNextTaskSameVehicle() {
		for (int i = 0; i < nbrTasks; i++) {
			if(nextTask[i] == -1) { //final task so don't need to check vehicle of nexttask
				continue;
			}
			if (vehicle[nextTask[i]] != vehicle[i]) {
				return false;
			}
		}
		
		return true;
	}
	
	//all tasks must be delivered
	private boolean checkAllTasksExecuted() {
		//count occurence of each tasks as next task. Last element is the occurrence of the null element
		int[] countOccurence = new int[nbrTasks + 1];
		for (int i = 0; i<nextTask.length; i++) {
			if (nextTask[i] == -1) {
				countOccurence[countOccurence.length-1]++;
			}
			else {
				countOccurence[nextTask[i]]++;
			}
		}
		for (int i = 0; i < countOccurence.length-1; i++) {
			if (countOccurence[i] != 1) {
				return false;
			}
		}
		if(countOccurence[countOccurence.length - 1] != nbrVehicles) {
			return false;
		}
		return true;
	}
	
	
	//The capacity of a vehicle cannot be exceeded
	private boolean checkCapacity() {
		for (int i = 0; i < weight.length; i++) {
			if (weight[i] > vehicleCapacity[vehicle[i]]) {
				return false;
			}				
		}
		return true;
	}
	
	//Check pickup and delivery of same vehicle
	private boolean checkPickupDeliveryVehicle() {
		for (int i = 0; i < vehicle.length; i += 2) {
			if(vehicle[i] != vehicle[i+1]) {
				return false;
			}
		}
		return true;
	}
	
	
	//Check if pickup of a task happens before the delivery of that task
	private boolean checkPickupBeforeDelivery() {
		for (int i = 0; i < time.length; i += 2) {
			if(time[i]>=time[i+1]) {
				return false;
			}
		}
		return true;
	}
	
	private void changingVehicle(Solution a, int v1, int v2) {
		Solution a1 = a;
		int t = nextTask[nbrTasks + v1];
		a1.nextTask[nbrTasks+v1] = a1.nextTask[t];
		a1.nextTask[t] = a1.nextTask[nbrTasks+v2];
		a1.nextTask[v2] = t;
	}
	
	private void updateTime(Solution a, int v) {
		
	}
	
}
