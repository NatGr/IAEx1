package centralized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

/*
 * altought the methods implements cloneable, the clone method only performs a shallow copy,
 * see the copy method for a deep copy
 */
public class Solution implements Cloneable {
	// what we call Task in the following is either a task delivery or a task pickup
	private final int nbrVehicles, nbrTasks;
	private final int[] weight; // the weight of every Task
	private final City[] city; // the city corresponding to every Task and every Vehicle (initial city)
	// that array is orderer by : [Task_0 (pickup), Task_0 (delivery), ..., Task_n
	// (delivery), Vehicle_0, ..., Vehicle_m]
	private final int[] vehicleCapacity; // initial capacity of every vehicle
	private final int[] vehicleCostPerKm; // cost per Km of every vehicle

	// solution characteristics:
	private int[] remainingCapacity; // the remaining capacity of the vehicle when he accepts the corresponding Task
	private int[] vehicle; // the offset of the vehicle corresponding the the Task
	private int[] time; // the time offset of the given Task
	int[] nextTask; // on position i, you find the offset of the next Task or -1 if the next Task is
					// null
	// that array is orderer by : [Task_0 (pickup), Task_0 (delivery), ..., Task_n
	// (delivery), Vehicle_0, ..., Vehicle_m]
	double score; // score of the current solution

	/*
	 * initializes all problem variables and generates a first valid solution if
	 * there are tasks that no vehicle is able to carry, the problem is unsolvable
	 * and we throw an IllegalArgumentException
	 */
	public Solution(List<Task> tasks, List<Vehicle> vehicles, long seed) throws IllegalArgumentException {
		nbrVehicles = vehicles.size();
		nbrTasks = 2 * tasks.size();
		weight = new int[nbrTasks];
		city = new City[nbrTasks + nbrVehicles];
		vehicleCapacity = new int[nbrVehicles];
		vehicleCostPerKm = new int[nbrVehicles];
		// TODO: init remainingCapacity, vehicle, time
		nextTask = new int[nbrTasks + nbrVehicles];
		Arrays.fill(nextTask, -1); // default value is "next task is null"

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
				throw new IllegalArgumentException(
						"One of the tasks has a weight that is too big for any vehicle, the problem is unsolvable.");
			}
			weight[deliver] = -weight[pickup]; // delivering a task is equivalent to taking a task of negtaive capacity
			city[pickup] = tasks.get(i).pickupCity;
			city[deliver] = tasks.get(i).deliveryCity;
		}

		// starting solution, we assign the tasks randomly but so as to respect the
		// constraints:
		Random generator = new Random(seed);
		int[] nextTaskVehicle = new int[nbrVehicles]; // array containing the number of vehicles
		for (int i = 0; i < nbrVehicles; i++) {
			nextTaskVehicle[i] = nbrTasks + i; // offset in the nextTask array
		}

		List<Integer> permutation = new ArrayList<Integer>(); // random ordering of the tasks
		for (int i = 0; i < tasks.size(); i++) {
			permutation.add(i);
		}
		Collections.shuffle(permutation, generator);

		for (int i : permutation) {
			do {
				int vehicle = generator.nextInt(nbrVehicles);
				if (vehicleCapacity[vehicle] >= tasks.get(i).weight) {
					nextTask[nextTaskVehicle[vehicle]] = 2 * i; // pickup task i
					nextTask[2 * i] = 2 * i + 1; // delivering task i
					nextTaskVehicle[vehicle] = 2 * i + 1;
					// TODO: Don't you forget to update vehicleCapacity?
					break;
				}
			} while (true);
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
			for (int current = offset, next = nextTask[offset]; next != -1; current = next, next = nextTask[next]) {
				vehicleDrivenDistance += city[current].distanceTo(city[next]);
			}
			score += vehicleCostPerKm[i] * vehicleDrivenDistance;
		}
	}

	// nextTask(t)!=t
	private boolean checkNextEqualsSelf() {
		// i is index of task
		for (int i = 0; i < nbrTasks; i++) {
			if (nextTask[i] == i)
				return false;
		}
		return true;
	}

	// nextTask(v_k)=t_j => time(t_j)=1
	private boolean checkNextVehicleTime() {
		// iterate over all vehicles
		for (int i = nbrTasks; i < nextTask.length; i++) {
			// nextTask[i] is the first task of vehicle i-nbrTasks
			if (time[nextTask[i]] != 1) {
				if (nextTask[i] != -1) { // possibility that a vehicle doesn't execute a task at all
					return false;
				}
			}
		}
		return true;
	}

	// nextTask(t_i)=t_j => time(t_j)=time(t_i)+1
	private boolean checkTimeNextTask() {
		for (int i = 0; i < nbrTasks; i++) {
			if (nextTask[i] == -1) { // final task so don't need to check time of nexttask
				continue;
			}
			if (time[i] + 1 != time[nextTask[i]]) {
				return false;
			}
		}
		return true;
	}

	// nextTask(v_k)=t_j => vehicle(t_j)=v_k
	private boolean checkVehicleTaskPair() {
		for (int i = nbrTasks; i < nbrVehicles; i++) {
			if (vehicle[nextTask[i]] != i) {
				return false;
			}
		}
		return true;
	}

	// nextTask(t_i)=t_j => vehicle(t_j) = vehicle(t_i)
	private boolean checkNextTaskSameVehicle() {
		for (int i = 0; i < nbrTasks; i++) {
			if (nextTask[i] == -1) { // final task so don't need to check vehicle of nexttask
				continue;
			}
			if (vehicle[nextTask[i]] != vehicle[i]) {
				return false;
			}
		}

		return true;
	}

	// all tasks must be delivered
	private boolean checkAllTasksExecuted() {
		// count occurence of each tasks as next task. Last element is the occurrence of
		// the null element
		int[] countOccurence = new int[nbrTasks + 1];
		for (int i = 0; i < nextTask.length; i++) {
			if (nextTask[i] == -1) {
				countOccurence[countOccurence.length - 1]++;
			} else {
				countOccurence[nextTask[i]]++;
			}
		}
		for (int i = 0; i < countOccurence.length - 1; i++) {
			if (countOccurence[i] != 1) {
				return false;
			}
		}
		if (countOccurence[countOccurence.length - 1] != nbrVehicles) {
			return false;
		}
		return true;
	}

	// The capacity of a vehicle cannot be exceeded
	private boolean checkCapacity() {
		for (int i = 0; i < weight.length; i++) {
			if (weight[i] > vehicleCapacity[vehicle[i]]) {
				return false;
			}
		}
		return true;
	}

	// Check pickup and delivery of same vehicle
	private boolean checkPickupDeliveryVehicle() {
		for (int i = 0; i < vehicle.length; i += 2) {
			if (vehicle[i] != vehicle[i + 1]) {
				return false;
			}
		}
		return true;
	}

	// Check if pickup of a task happens before the delivery of that task
	private boolean checkPickupBeforeDelivery() {
		for (int i = 0; i < time.length; i += 2) {
			if (time[i] >= time[i + 1]) {
				return false;
			}
		}
		return true;
	}
	
	private void chooseNeighbours(Solution a) {
		ArrayList<Solution> N = new ArrayList<Solution>();
		Random generator = new Random();
		int v_i = generator.nextInt(nbrVehicles);
		while(a.nextTask[nbrTasks+v_i]==-1) {
			v_i = generator.nextInt(nbrVehicles);
		}
		
		
		
		for (int i=0; i<nbrVehicles; i++) {
			if(i!=v_i) {
				int t = a.nextTask[nbrTasks+v_i];
				if (weight[t]<= vehicleCapacity[v_i]) { //TODO: This is how it's stated in the algorithm but shouldn't this be remainingCapacity?
					//TODO: create clone of current solution before proceding
				}
			}
		}
		
		
	}

	/**
	 * @param a: solution which is already a clone. This one can thus be edited during this procedure
	 * @param v1: first vehicle. The index of the vehicles goes from 0 to nbrVehicle
	 * @param v2: second vehicle. The index of the vehicles goes from 0 to nbrVehicle
	 * This procedure will take the first task of vehicle v1 and insert it in the array of tasks of v2 at the first position.
	 * As the first task always is a pickuptask, we move the corresponding delivery task from vehicle as well and place it immediatly after the pickup task
	 */
	//TODO: Think about a good policy for the position of the delivery task
	//TODO: (Optional) Make this more dynamic such that any task can be changed from vehicle.
	private void changingVehicle(Solution a, int v1, int v2) {
		if(v1==v2) return;
		int t_pickup = a.nextTask[nbrTasks + v1];
		int t_delivery = t_pickup + 1;
		a.nextTask[nbrTasks + v1] = a.nextTask[t_pickup];
		
		int t_i = nbrTasks+v1;
		while(a.nextTask[t_i] != t_delivery) {
			t_i = a.nextTask[t_i];
		}
		a.nextTask[t_i]=a.nextTask[t_delivery];
		
		a.nextTask[t_pickup]=t_delivery;
		a.nextTask[t_delivery] = a.nextTask[nbrTasks+v2];
		a.nextTask[t_pickup] = t_delivery;
		a.nextTask[nbrTasks + v2] = t_pickup;
		updateTime(a, v1);
		updateTime(a, v2);
		a.vehicle[t_pickup] = v2;
		a.vehicle[t_delivery] = v2;
	}

	/**
	 * @param a: solution which is already a clone. This one can thus be edited
	 *        during this procedure
	 * @param v: vehicle This procedure will set the time values of v in the right
	 *        way again. A
	 */
	private void updateTime(Solution a, int v) {
		// TODO: This will correctly change the time values of the solution a given as
		// argument, right?
		int t_i = a.nextTask[v];
		if (t_i >= 0) {
			a.time[t_i] = 0; // First task gets time value 0
			int t_j = a.nextTask[t_i];
			while (t_j != -1) {
				t_j = a.nextTask[t_i];
				if (t_j != -1) {
					a.time[t_j] = a.time[t_i] + 1;
					t_i = t_j;
				}
			}
		}
	}

	/**
	 * @param a: solution which is already a clone. This one can thus be edited
	 *        during this procedure
	 * @param v: index of the vehicle. Vehicle index starts from 0. The index of
	 *        this vehicle in the array nextTasks is thus nbrTasks + v
	 * @param tIdx1: time index of task 1. Vehicle v would execute task 1 on time
	 *        index tIdx1
	 * @param tIdx2: time index of task 2. Vehicle v would execute task 2 on time
	 *        index tIdx2 This procedure will swap two tasks executed by vehicle v
	 *        It is important to verify that suddenly the delivery of the task does
	 *        not happen before the pickup
	 */
	private void changingTaskOrder(Solution a, int v, int tIdx1, int tIdx2) {
		// Assume that Solution a is already a clone and thus can be altered during this
		// procedure.
		int tPre1 = nbrTasks + v;
		int t1 = a.nextTask[tPre1];
		int count = 0;
		int index_first_task = Math.min(tIdx1, tIdx2);
		int index_second_task = Math.max(tIdx1, tIdx2);
		while (count < index_first_task) {
			tPre1 = t1;
			t1 = a.nextTask[t1];
			count++;
		} // At this moment t1 is pointing at the task on time instnat tIdx1
		int tPost1 = a.nextTask[t1];
		int tPre2 = t1;
		int t2 = a.nextTask[tPre2];
		count++;
		while (count < index_second_task) {
			tPre2 = t2;
			t2 = a.nextTask[t2];
			count++;
		}
		int tPost2 = a.nextTask[t2];
		// At this point we have pointers at task 1 and 2 and the tasks following and
		// presceding them
		if (tPost1 == t2) {
			a.nextTask[tPre1] = t2;
			a.nextTask[t2] = t1;
			a.nextTask[t1] = tPost2;
		} else {
			a.nextTask[tPre1] = t2;
			a.nextTask[tPre2] = t1;
			a.nextTask[t2] = tPost1;
			a.nextTask[t1] = tPost2;
		}
		// TODO: Check if delivery doesn't suddenly happen before pickup!!
		updateTime(a, v);

	}
	
	/**
	 * makes a deep/shallow (depending on the arguments) copy of the object
	 * @param newRemainingCapacity: wether we perform a deep copy of remainingCapacity or a shallow one
	 * @param newVehicle: wether we perform a deep copy of vehicle or a shallow one
	 * @param newTime: wether we perform a deep copy of time or a shallow one
	 * @param newNextTask: wether we perform a deep copy of nextTask or a shallow one
	 * @throws CloneNotSupportedException
	 */
	private Solution copy(boolean newRemainingCapacity, boolean newVehicle, boolean newTime, boolean newNextTask) throws CloneNotSupportedException {
		Solution copy = (Solution) this.clone(); // shallow copy
		if (newRemainingCapacity) {
			copy.remainingCapacity = Arrays.copyOf(this.remainingCapacity, this.remainingCapacity.length);
		}
		if (newVehicle) {
			copy.vehicle = Arrays.copyOf(this.vehicle, this.vehicle.length);
		}
		if (newTime) {
			copy.time = Arrays.copyOf(this.time, this.time.length);
		}
		if (newNextTask) {
			copy.nextTask = Arrays.copyOf(this.nextTask, this.nextTask.length);
		}
		return copy;
	}

}
