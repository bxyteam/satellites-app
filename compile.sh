
echo "Compiling..."
rm -rf target

mvn clean package

mvn clean install

echo "Creating runnable directory"
mkdir -p target/runnable

echo "Copying bindist/lib to runnable/lib"
cp -r target/bindist/lib target/runnable/lib

echo "Copying classes to runnable/classes"
cp -r target/classes target/runnable/classes

echo "Copying scripts to runnable"
cp -r target/classes/scripts/. target/runnable

echo "Remove files and folders"
rm -rf target/runnable/classes/scripts/tini target/runnable/classes/scripts/web

echo "Done"
