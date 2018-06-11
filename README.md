# Cricket DashBoard POC

this POC is created to showcase the integration of the following components:

* Kafka
* Java
* scala
* spark
* solr
* banana

## Architecture
The Arch of this POC is very simple.following are the components of this Arch : 
  * Producer : The 'CriProducer' Java project is responsible for creating a stream of data in JSON format and pushing it to kafka.
  The producer will ask for the following info :
    1. Number of overs
    2. bowlers Name
    3. Batsmen name
 
 After this the producer will start producing random messages and feed it into the Kafka topic.
 
 * Consumer : The 'CriConsumer' Scala project will consume the json data from kafka and process it to calculate the Batting strike rate , bowling Avg , etc and store the data in SOLR.
 
 * SOLR : We have created an CORE in SOLR to store the streming data.
 
 * Banana Dashboard : The Banana Dashboard will read the data in the solr periodically and create varous graphs on top of it.
    

![alt text](https://github.com/nautiyal-sarthak/CricketDashBoard_POC/blob/master/screenshots/Screen%20Shot%202018-06-10%20at%206.37.16%20PM.png "Logo Title Text 1")


## Screenshots
1. sample output of the producer : 
![alt text](https://github.com/nautiyal-sarthak/CricketDashBoard_POC/blob/master/screenshots/Screen%20Shot%202018-06-10%20at%206.15.26%20PM.png)

2. samaple data stored in SOLR after eing processed by the consumer :
![alt text](https://github.com/nautiyal-sarthak/CricketDashBoard_POC/blob/master/screenshots/Screen%20Shot%202018-06-10%20at%206.16.31%20PM.png)

3. Various graphs build on top of SOLR :

* Runs scored per Ball
![alt text](https://github.com/nautiyal-sarthak/CricketDashBoard_POC/blob/master/screenshots/Screen%20Shot%202018-06-10%20at%206.17.30%20PM.png)

* StrikeRate of all the batsmen per ball faced
![alt text](https://github.com/nautiyal-sarthak/CricketDashBoard_POC/blob/master/screenshots/Screen%20Shot%202018-06-10%20at%206.17.42%20PM.png)

* Bowling avg of all the bowlers per balls bowled
![alt text](https://github.com/nautiyal-sarthak/CricketDashBoard_POC/blob/master/screenshots/Screen%20Shot%202018-06-10%20at%206.17.52%20PM.png)

* Scatter plot for ploting the runs scored by each batsman.
![alt text](https://github.com/nautiyal-sarthak/CricketDashBoard_POC/blob/master/screenshots/Screen%20Shot%202018-06-10%20at%206.18.13%20PM.png)

* Line plot for the total runs scored and run rate 
![alt text](https://github.com/nautiyal-sarthak/CricketDashBoard_POC/blob/master/screenshots/Screen%20Shot%202018-06-10%20at%206.18.22%20PM.png)

* Pie chart to show how many 4s,6s etc have been scored.
![alt text](https://github.com/nautiyal-sarthak/CricketDashBoard_POC/blob/master/screenshots/Screen%20Shot%202018-06-10%20at%206.18.35%20PM.png)
