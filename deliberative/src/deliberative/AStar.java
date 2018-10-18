package deliberative;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import logist.plan.Plan;

public class AStar implements Algorithm {
	
	@Override
	public Plan plan(State initState) {
		PriorityQueue<State> priorityQueue = new PriorityQueue<State>();
		Map<State, Double> seenStates = new HashMap<State, Double>();
		
		State state = initState;
		while(state != null) {
			if (state.isTerminal()) {
				return state.getPlan();
			} else {
				for (State s: state.createChildren(false)) {
					Double prevScore = seenStates.get(s);
					if (prevScore == null) {
						seenStates.put(s, s.cost + s.heuristic);
						priorityQueue.add(s);
					} else if (s.cost + s.heuristic < prevScore) {
						seenStates.put(s, s.cost + s.heuristic);
						priorityQueue.remove(s); // TODO: check it doesn't remove something with equal comparator or with ==
						priorityQueue.add(s);
					} // else do nothing
				}
				state = priorityQueue.poll();
			}
		}
		return null; // should not happen
	}
}
