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
		ArrayList<State> finalStates = new ArrayList<State>();
		Map<State, Double> seenStates = new HashMap<State, Double>();
		
		State state = initState;
		// BFS
		while(state != null) {
			if (state.isTerminal()) {
				finalStates.add(state);
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
		
		// finding best plan, i.e. the one leading to a final state with the smallest distance:
		State bestState = null;
		double bestDistance = Double.MAX_VALUE, currentDistance;
		for (State finalState: finalStates) {
			currentDistance = finalState.cost;
			if (currentDistance < bestDistance) {
				bestDistance = currentDistance;
				bestState = finalState;
			}
		}
		if (bestState != null) {
			return bestState.getPlan();
		} else { // should not happen
			return null;
		}
	}

}
