# Build ClassPath
for i in `ls ./lib/*.jar`
do
  CLASSPATH=$CLASSPATH$i":"
done
CLASSPATH=conf:classes:$CLASSPATH

# Execute Java Application
echo ""
echo `date`" - Starting Satellite Application"
echo `java -version`
nohup java -Xms40M -Xmx60M -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=2000 -cp $CLASSPATH com.browxy.satellites.run.SatelliteRunner $1 > satellite.out 2>satellite.err < /dev/null & echo $! > satellite.pid &
