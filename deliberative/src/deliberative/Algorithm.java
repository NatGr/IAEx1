package deliberative;

import java.util.List;

import logist.plan.Action;
import logist.plan.Plan;
import logist.topology.Topology.City;

public abstract class Algorithm {
	
	abstract public Plan plan(State initState);
	
	/* create a plan from the given list of actions and the initial city,
	the actions are ordered from last (in time) to first */
	protected Plan getPlanFromActions(List<Action> actions, City initialCity) {
		Plan plan = new Plan(initialCity);
		for (int i = actions.size() - 1; i >= 0; i--) {
			plan.append(actions.get(i));
		}
		return plan;
		
	}
}
