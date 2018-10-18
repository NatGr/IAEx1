package deliberative;

import logist.plan.Plan;
import logist.task.Task;
import logist.topology.Topology.City;

public class Naive implements Algorithm{

	@Override
	public Plan plan(State initState) {
		City current = initState.getCity();
		Plan plan = new Plan(current);
		
		for (Task task : initState.getAvailableTasks()) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}
}
