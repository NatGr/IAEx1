package auction;

import logist.topology.Topology.City;

/*
 * class containing the probability of generating a task (the proba should be divided 
 * by the number of cities to be the correct proba) as well as the task and pickup and deliver city
 */
public class TaskProba implements Comparable<TaskProba>{
	double proba;
	City pickupCity;
	City deliveryCity;
	int weight;
	
	TaskProba(double proba, City pickupCity, City deliveryCity, int weight) {
		this.proba = proba;
		this.pickupCity = pickupCity;
		this.deliveryCity = deliveryCity;
		this.weight = weight;
	}
	
	/* return 1 if this proba is smaller than tp2 troba, -1 if bigger and 0 if they are equal */
	@Override
	public int compareTo(TaskProba tp2) {
		double diff = this.proba - tp2.proba;
		if (diff < 0) {
			return 1;
		} else if (diff > 0) {
			return -1;
		} else {
			return 0;
		}
	}
}
