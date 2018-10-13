package deliberative;

import java.util.ArrayList;
import java.util.List;

import logist.plan.Action;
import logist.plan.Plan;

public class AStar extends Algorithm {

	@Override
	public Plan plan(State initState) {
		List<Action> actions = new ArrayList<Action>();
		// TODO Auto-generated method stub
		
		return getPlanFromActions(actions, initState.city);
	}

}
