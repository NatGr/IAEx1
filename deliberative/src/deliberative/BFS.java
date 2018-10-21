package deliberative;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import logist.plan.Plan;

public class BFS implements Algorithm {

	@Override
	public Plan plan(State initState) {
		Queue<State> queue = new LinkedList<State>();
		Map<State, Double> seenStates = new HashMap<State, Double>();
		State bestFinalState = null;
		
		State state = initState;
		// BFS
		while(state != null) {
			if (state.isTerminal()) {
				if (bestFinalState == null || state.cost < bestFinalState.cost) {
					bestFinalState = state;
				}
			} else {
				for (State s: state.createChildren(false)) {
					Double prevScore = seenStates.get(s);  // will get a state that is equal in term of city, 
					// availableTasks and pickedUpTasks but not necessarily cost
					if (prevScore == null) {
						seenStates.put(s, s.cost);
						queue.add(s);
					} else if (s.cost < prevScore) {
						seenStates.put(s, s.cost);
						queue.remove(s); // removes the state equivalent to s but with a lower score
						queue.add(s);
					}
				}
			}
			state = queue.poll();
		}
		if (bestFinalState != null) {
			return bestFinalState.getPlan();
		} else { // should not happen
			return null;
		}
	}

}
