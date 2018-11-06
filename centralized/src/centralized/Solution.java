package centralized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;


public class Solution implements Cloneable, Comparable<Solution> {
	// what we call Task in the following is either a task delivery or a task pickup
	private final int nbrVehicles, nbrTasks;
	private final int[] weight; // the weight of every Task
	private final City[] city; // the city corresponding to every Task and every Vehicle (initial city)
	// that array is orderer by : [Task_0 (pickup), Task_0 (delivery), ..., Task_n
	// (delivery), Vehicle_0, ..., Vehicle_m]
	private final int[] vehicleCapacity; // initial capacity of every vehicle
	private final int[] vehicleCostPerKm; // cost per Km of every vehicle

	// solution characteristics:
	int[] nextTask; // on position i, you find the offset of the next Task or -1 if the next Task is null
	// that array is orderer by : [Task_0 (pickup), Task_0 (delivery), ..., Task_n
	// (delivery), Vehicle_0, ..., Vehicle_m]
	double cost; // cost of the current solution

	/*
	 * initializes all problem variables and generates a first valid solution if
	 * there are tasks that no vehicle is able to carry, the problem is unsolvable
	 * and we throw an IllegalArgumentException
	 */	
	public Solution(List<Task> tasks, List<Vehicle> vehicles, long seed) throws IllegalArgumentException {
		//Initialization		
		nbrVehicles = vehicles.size();
		nbrTasks = 2 * tasks.size();
		weight = new int[nbrTasks];
		city = new City[nbrTasks + nbrVehicles];
		vehicleCapacity = new int[nbrVehicles];
		vehicleCostPerKm = new int[nbrVehicles];
		nextTask = new int[nbrTasks + nbrVehicles];
		Arrays.fill(nextTask, -1); // default value is "next task is null"
		int maxCapacity = Integer.MIN_VALUE; //Maximum capacity among all vehicles
		
		//Vehicle specific initialization
		for (int i = 0; i < nbrVehicles; i++) {
			city[nbrTasks + i] = vehicles.get(i).homeCity();
			vehicleCapacity[i] = vehicles.get(i).capacity();
			maxCapacity = Math.max(maxCapacity, vehicleCapacity[i]);
			vehicleCostPerKm[i] = vehicles.get(i).costPerKm();
		}

		//Task specific initialization
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

		/* 
		 * Starting solution, we assign the tasks randomly but so as to respect the	constraints:
		 */
		
		//Create relation between position of vehicle in nextTask array and (vehicleCapacity, vehicleCostPerKm)
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
					break;
				}
			} while (true);
		}

		// compute our solution's score
		computeCost();
	}
	
	/*
	 * generates neighbours of the current solution by moving the first task from vehicle A to other vehicles
	 * and by moving a pickup and deliver tasks inside of a vehicle.
	 */
	public ArrayList<Solution> generateNeighbours() {
		ArrayList<Solution> Neighbourgs = new ArrayList<Solution>();
		Random generator = new Random();
		
		// moving tasks from a vehicle to another
		int v1;
		do {
			v1 = generator.nextInt(nbrVehicles);
		} while(nextTask[nbrTasks+v1] == -1);  // find a vehicle with at least two tasks (pickup and delivery)
		
		// find a pickup and delivery task at a random offset
		int nbrTasksVehicle = 0, beforePickup = -1, offsetPickup;
		for (int i = nbrTasks+v1; nextTask[i] != -1; i = nextTask[i], nbrTasksVehicle++) {} // counts the nbr of tasks of a vehicle
		offsetPickup = generator.nextInt(nbrTasksVehicle/2);
		
		for (int task = nbrTasks+v1; task != -1; task = nextTask[task]) {
			if (nextTask[task] % 2 == 0 && offsetPickup-- == 0) { // if we have a pickup task, we check if we will move that one and then we decrease offsetPickup
				beforePickup = task;
				break;
			}
		}
		
		for (int v2 = 0; v2 < nbrVehicles; v2++) {
			if (v2 != v1) {
				if (weight[nextTask[beforePickup]] <= vehicleCapacity[v2]) {
					/*
					 * Check with vehicleCapacity instead of remainingCapacity because the delivery happens immediatly after pickup
					 * */
					appendchangingVehicleToN(Neighbourgs, v1, v2, beforePickup);
				}
			}
		}
		
		// moving tasks order inside of a vehicle
		int next, iter = 0;
		nbrTasksVehicle = 0;
		do {
			v1 = generator.nextInt(nbrVehicles);
			next = nextTask[nbrTasks+v1];
			iter += 1;
		} while ((next == -1 || nextTask[nextTask[next]] == -1) && iter < 2*nbrVehicles); // find a vehicle with at least four tasks (two times pickup and delivery)
		// we stop after having tried 2*nbrVehicles times since a vehicle with at lest four tasks might not exist
		
		if (next != -1 && nextTask[nextTask[next]] != -1) {  // if we found a vehicle 
			for (int i = nbrTasks+v1; nextTask[i] != -1; i = nextTask[i], nbrTasksVehicle++) {} // counts the nbr of tasks of a vehicle
	
			int[] timeVehicle = new int[nbrTasksVehicle];  // If task t (index in nextTask) is executed as i'th task by vehicle v, we will have value t at index i
			int[] remainingCapacity = new int[nbrTasksVehicle];  // At index i, you have the remaining capacity after executing task i
			int task = nextTask[nbrTasks+v1], pickupTOffset = -1, deliveryTOffset = -1;
			offsetPickup = generator.nextInt(nbrTasksVehicle/2);  // number of Pickup Tasks we will see before taking the one we will change of vehicle
			timeVehicle[0] = task;
			if (offsetPickup-- == 0) {
				pickupTOffset = 0;
			}
			remainingCapacity[0] = vehicleCapacity[v1] - weight[task];
			task = nextTask[task];
			for (int i = 1; task != -1; task = nextTask[task], i++) { // fill the timeVehicle 
				// and remainingCapacity arrays
				timeVehicle[i] = task;
				remainingCapacity[i] = remainingCapacity[i-1] - weight[task];
				if (task % 2 == 0 && offsetPickup-- == 0) { // if we have a pickup task, we check if we will move that one and then we decrease offsetPickup
					pickupTOffset = i;
				} else if (pickupTOffset >= 0 && timeVehicle[pickupTOffset]+1 == task) { // we have selected our pickupTask and we can get the corresponding deliveryTask
					deliveryTOffset = i;
				}
			}
			appendchangingTaskOrderToN(Neighbourgs, v1, pickupTOffset, deliveryTOffset, timeVehicle, remainingCapacity);
			
			// we swap two tasks in the same vehicle:
			offsetPickup = generator.nextInt(nbrTasksVehicle/2);
			int offsetPickup2 = generator.nextInt(nbrTasksVehicle/2);
			while (offsetPickup2 == offsetPickup) {
				offsetPickup2 = generator.nextInt(nbrTasksVehicle/2);
			}
			appendSwapTwoTasksToN(Neighbourgs, v1, offsetPickup, offsetPickup2, timeVehicle);
		}
		return Neighbourgs;
	}

	/**
	 * This procedure will add a new solution to N where the first pickup task 
	 * (and the corresponding delivery task) will have been moved from v1 to the two first
	 * positions of v2
	 * 
	 * @param N: the arraylist we will append the new solutions to
	 * @param v1: first vehicle. The index of the vehicles goes from 0 to nbrVehicle - 1
	 * @param v2: second vehicle. The index of the vehicles goes from 0 to nbrVehicle - 1
	 * @param beforePickup: the task preceding the pickup task we want to move from v1 to v2 (might be v1's index rather than a task)
	 */
	private void appendchangingVehicleToN(ArrayList<Solution> N, int v1, int v2, int beforePickup) {
		try {
			Solution newSol = (Solution) this.clone();
			// remove these tasks from v1
			int pickup = nextTask[beforePickup];
			int delivery = pickup + 1;			
			
			// remove these tasks from v1
			if (nextTask[pickup] != delivery) {  // delivery is not second action performed by vehicle
				int beforeDelivery = nextTask[pickup];
				for (; nextTask[beforeDelivery] != delivery; beforeDelivery = nextTask[beforeDelivery]) {} // set beforeDelivery to the predecessor of delivery
				
				newSol.nextTask[beforePickup] = nextTask[pickup];
				newSol.nextTask[beforeDelivery] = nextTask[delivery];  // might be -1 
			} else {
				newSol.nextTask[beforePickup] = nextTask[delivery];
			}
			
			newSol.nextTask[nbrTasks + v2] = pickup;
			newSol.nextTask[pickup] = delivery;
			newSol.nextTask[delivery] = nextTask[nbrTasks + v2];  // the previously first task of v2
			newSol.computeCost();  // we shall not forget to update the score of newSol
			N.add(newSol); 
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates new Solutions where a pickup and deliver tasks are moved inside of a vehicle priority list
	 * before and after as much as possible as long as they don't rape the constraints.
	 * Ex: if t_pick happens 3rd and can happen 1st without raping constraints, a solution will contain
	 * t_pick in first position, another t_pick in second position, and others where t_pick was postponed, t_del was preponed
	 * and t_del was postponed
	 * The solutions are added to N
	 * 
	 * @param N: the arraylist we will append the new solutions to
	 * @param v: index of the vehicle inside of which task order will change
	 * @param pickupTOffset: time index of pickup task. Vehicle v would execute pickup task on time pickupTOffset
	 * @param deliveryTOffset: time index of deliver task. Vehicle v would execute deliver task on time deliverTOffset
	 * @param timeVehicle: array whose index are the time offsets and elements the tasks happening at these offsets
	 * @param remainingCapacity: the remaining capacity of the vehicle at each time offset
	 */
	private void appendchangingTaskOrderToN(ArrayList<Solution> N, int v, int pickupTOffset, int deliveryTOffset,
			int[] timeVehicle, int[] remainingCapacity) {
		int pickup = timeVehicle[pickupTOffset], delivery = timeVehicle[deliveryTOffset];
		int prevPickup = (pickupTOffset == 0) ? nbrTasks+v : timeVehicle[pickupTOffset - 1];
		try {
			/* pickuing up earlier, we can prepone the picking up from one element iteratively
			 * as long as we don't go before a deliveryTask A such that we could not have picked
			 * up our task before delivering A
			*/
			for(int i = pickupTOffset - 1; i >= 0; i--) {
				if (timeVehicle[i] % 2 == 1 && remainingCapacity[i-1] < weight[pickup]) {
					break;
				} else {  // we can pickup one step earlier
					Solution newSol = (Solution) this.clone();
					if (i == 0) { // we will place the pickup first
						newSol.nextTask[nbrTasks+v] = pickup;
						newSol.nextTask[pickup] = nextTask[nbrTasks+v];
					} else {
						newSol.nextTask[timeVehicle[i-1]] = pickup;
						newSol.nextTask[pickup] = timeVehicle[i];
					}
					newSol.nextTask[timeVehicle[pickupTOffset-1]] = timeVehicle[pickupTOffset+1];  // skip pickup
					newSol.computeCost();
					N.add(newSol);
				}
			}
			
			/* picking up later, we can postpone the pickup as long as we don't pickup our task after
			 * having delivered it
			 */
			for (int i = pickupTOffset + 1; i < deliveryTOffset; i++) {
				Solution newSol = (Solution) this.clone();
				newSol.nextTask[prevPickup] = nextTask[pickup];
				newSol.nextTask[timeVehicle[i]] = pickup;
				newSol.nextTask[pickup] = timeVehicle[i+1];
				newSol.computeCost();
				N.add(newSol);
			}
			
			/* delivering earlier, we can prepone the delivery as long as we don't deliver before having
			 * picked up
			 */
			for (int i = deliveryTOffset - 1; i > pickupTOffset; i--) {
				Solution newSol = (Solution) this.clone();
				newSol.nextTask[timeVehicle[i-1]] = delivery;
				newSol.nextTask[timeVehicle[deliveryTOffset-1]] = (deliveryTOffset == timeVehicle.length - 1) ? -1 : timeVehicle[deliveryTOffset+1]; // in case
				// the delivery is the last task
				newSol.nextTask[delivery] = timeVehicle[i];
				newSol.computeCost();
				N.add(newSol);
			}
			
			/* delivering later, we can postpone the delivery from one element iteratively as
			 * long as we don't go after a pickup task that we could not have picked up before delivering 
			 */
			for (int i = deliveryTOffset + 1; i < timeVehicle.length; i++) {
				if (timeVehicle[i] % 2 == 0 && remainingCapacity[i] < -weight[delivery]) { // the weight of a 
					// delivery is defined as a negative number, thus we add a "-"
					break;
				} else {
					Solution newSol = (Solution) this.clone();
					newSol.nextTask[timeVehicle[deliveryTOffset-1]] = timeVehicle[deliveryTOffset+1];
					newSol.nextTask[timeVehicle[i]] = delivery;
					newSol.nextTask[delivery] = nextTask[timeVehicle[i]];
					newSol.computeCost();
					N.add(newSol);
				}
			}
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * swaps two tasks (pickup1 <-> pickup2 and delivery1 <-> delivery2) and the new solution to N if it
	 * is valid
	 * 
	 * @param N: the arraylist we will append the new solution to
	 * @param v: index of the vehicle inside of which tasks will be swapped
	 * @param offsetPickup1: number of pickup the vehicle did before picking up task 1
	 * @param offsetPickup2: number of pickup the vehicle did before picking up task 2
	 * @param timeVehicle: array whose index are the time offsets and elements the tasks happening at these offsets
	 */
	private void appendSwapTwoTasksToN(ArrayList<Solution> N, int v, int offsetPickup1, int offsetPickup2, int[] timeVehicle) {
		try {
			int offsetTPickup1 = -1, offsetTPickup2 = -1, offsetTDeliver1 = -1, offsetTDeliver2 = -1;
			
			// find the given tasks
			for(int i = 0; i < timeVehicle.length; i++) { 
				if (timeVehicle[i] % 2 == 0) {
					if (offsetPickup1-- == 0) {
						offsetTPickup1 = i;
					}
					if (offsetPickup2-- == 0) {
						offsetTPickup2 = i;
					}
				}
				if (offsetTPickup1 >= 0 && timeVehicle[i] == timeVehicle[offsetTPickup1] + 1) {
					offsetTDeliver1 = i;
				} else if (offsetTPickup2 >= 0 && timeVehicle[i] == timeVehicle[offsetTPickup2] + 1) {
					offsetTDeliver2 = i;
				}
			}
			
			// swap the tasks, we will reconstruct the nextTask array from 0 for v1 (simpler that perfomring two swaps)
			Solution newSol = (Solution) this.clone();
			for(int task = nbrTasks+v, i = 0; task != -1; task = newSol.nextTask[task], i++) {
				if (i == offsetTPickup1) { // swap p1 and p2
					newSol.nextTask[task] = timeVehicle[offsetTPickup2];
				} else if (i == offsetTPickup2) {
					newSol.nextTask[task] = timeVehicle[offsetTPickup1];
				} else if (i == offsetTDeliver1) {
					newSol.nextTask[task] = timeVehicle[offsetTDeliver2];
				} else if (i == offsetTDeliver2) {
					newSol.nextTask[task] = timeVehicle[offsetTDeliver1];
				} else if (i == timeVehicle.length ){  // we have done our last task
					newSol.nextTask[task] = -1;
				} else {
					newSol.nextTask[task] = timeVehicle[i];
				}
			}
			
			
			// check if new solution is valid
			for(int task = newSol.nextTask[nbrTasks+v], remainingCapacity = vehicleCapacity[v]; task != -1; task = newSol.nextTask[task]) {
				remainingCapacity -= weight[task];
				if (remainingCapacity < 0) {
					return;
				}
			}
						
			newSol.computeCost();
			N.add(newSol);
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * @param tasks: the list of tasks that were given as arguments to the constructor we come from tasks is of type ArrayList to guarantee O(1) access
	 * @return the list of plans associated with the solution
	 */
	public List<Plan> getPlans(ArrayList<Task> tasks) {
		System.out.println("-----");
		for (int i: nextTask) {
			System.out.print(i + " ");
		}
		System.out.println("\n-----");
		List<Plan> plans = new ArrayList<Plan>();
        for (int i = nbrTasks; i < nextTask.length; i++) {
        	Plan plan = new Plan(city[i]);
        	City prevCity = city[i];
        	for (int j = nextTask[i]; j != -1; prevCity = city[j], j = nextTask[j]) {
        		for (City city : prevCity.pathTo(city[j])) {
					plan.appendMove(city);
				}
        		if (j % 2 == 0) {  // pickup task
        			plan.appendPickup(tasks.get(j/2)); // we get the corresponding offset in tasks by dividing by 2
        		} else {
        			plan.appendDelivery(tasks.get(j/2));
        		}
        	}
        	plans.add(plan);
        	
        }
		return plans;
	}
	

	/**
	 * Computes the score of the solution
	 */
	private void computeCost() {
		cost = 0;
		for (int i = 0, vehicleDrivenDistance, offset; i < nbrVehicles; i++) {
			// iterates over all the tasks the vehicle performs
			vehicleDrivenDistance = 0;
			offset = nbrTasks + i;
			for (int current = offset, next = nextTask[offset]; next != -1; current = next, next = nextTask[next]) {
				vehicleDrivenDistance += city[current].distanceTo(city[next]);
			}
			cost += vehicleCostPerKm[i] * vehicleDrivenDistance;
		}
	}
	

	/* 
	 * clone method that performs a deep cloning of the array nextTask
	 */
	public Object clone() throws CloneNotSupportedException {
		Solution clone = (Solution) super.clone();
		clone.nextTask = Arrays.copyOf(clone.nextTask, clone.nextTask.length);
		return clone;
	}
	

	/**
	 * @param array: array of tasks for which pos i needs to be switched with pos j
	 * @param i: index of task i
	 * @param j: index of task j
	 * This has nothing to do with the current class but java does not provide the functionality
	 */
	public static final void swap(int[] array, int i, int j) {
		int tmp = array[i];
		array[i] = array[j];
		array[j] = tmp;
	}

	// comparison so as to sort the elements from their score, the difference is rounded to -1, 0 or +1
	public int compareTo(Solution s2) {
		double diff = this.cost - s2.cost;
		if (diff < 0) {
			return -1;
		} else if (diff > 0) {
			return 1;
		} else {
			return 0;
		}
	}
}
