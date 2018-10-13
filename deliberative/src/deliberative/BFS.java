package deliberative;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import logist.plan.Action;
import logist.plan.Plan;

public class BFS extends Algorithm {

	@Override
	public Plan plan(State initState) {
		List<Action> actions = new ArrayList<Action>();
		Queue<State> queue = new LinkedList<State>();
		
		
		State state = null; // getting the optimal state from bfs
		
		
		while (state.parent != null) {
			actions.add(state.getPreviousAction());
			state = state.parent;
		}
		return getPlanFromActions(actions, initState.city);
	}

}
