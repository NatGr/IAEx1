# first argument: tournament name
# second argument: config file
java -jar logist/logist.jar -new $1 agents
java -jar logist/logist.jar -run $1 $2
java -jar logist/logist.jar -score $1
results='tournament/'$1'/results.txt'
cat echo $results
