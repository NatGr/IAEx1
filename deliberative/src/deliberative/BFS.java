package deliberative;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import logist.plan.Plan;

public class BFS implements Algorithm {

	@Override
	public Plan plan(State initState) {
		Queue<State> queue = new LinkedList<State>();
		ArrayList<State> finalStates = new ArrayList<State>();
		queue.add(initState);
		
		State state = initState;
		// BFS
		while(state != null) {
			if (state.isTerminal()) {
				finalStates.add(state);
			} else {
				for (State s: state.createChildren()) {
					boolean found = false;
					for (State inQueue: queue) { // we check if an equivalent state is not already in the queue
						if (s.equals(inQueue)) {
							found = true;
							/* /!\ the state we found does not necessarily have the same parent nor the same cost
							 * so if the cost of our state is smaller than the cost of the found state, we will
							 * modify the cost and parent of that state in the queue, this will not completely result
							 * in BFS since our state should be at the end of the queue but this doesn't change anything
							 * and is easier to code so it is done anyway
							 */
							if (inQueue.cost > s.cost) {
								inQueue.cost = s.cost;
								inQueue.parent = s.parent;
							}
							break;
						}
					}
					if (!found) {
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
