package deliberative;

import logist.plan.Plan;

public interface Algorithm {
	
	public Plan plan(State initState);
	
}
