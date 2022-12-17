# projetSD2022
projet de systemes distribuees



dans un terminal :
export JAVA_HOME=/usr/gide/jdk-1.8
export PATH=${JAVA_HOME}/bin:${PATH}
export HADOOP_CLASSPATH=${JAVA_HOME}/lib/tools.jar

puis se déplacer dans le fichier /te131323/hadoop/hadoop-2.10.2/src et écrire les lignes:
//ces lignes permettent de mettre en route le HDFS et le Yarn, retrouvable ensuite en localhost:8088 et localhost:50070


../bin/hdfs namenode -format

../sbin/start-dfs.sh

../sbin/start-yarn.sh

//Ces lignes permettent de configurer le programme afin d'ajouter les chemins d'entrées et de sorties du MapReduce

../bin/hadoop com.sun.tools.javac.Main  MRHITS.java

jar cf wc.jar MRHITS*.class

../bin/hadoop fs -mkdir /mrhits1

../bin/hadoop fs -mkdir /mrhits1/input

../bin/hadoop fs -put MRtest.txt /mrhits1/input

../bin/hadoop jar wc.jar MRHITS /mrhits1/input /mrhits1/output1

../bin/hadoop fs -cat /mrhits1/output1/part-r-00000
