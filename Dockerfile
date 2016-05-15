FROM ubuntu:16.04
RUN apt-get -y update
RUN apt-get install -y openjdk-8-jdk
RUN apt-get install -y maven
WORKDIR /opt/application
ADD . /opt/application
RUN mvn compile
EXPOSE 4567
CMD ["bash", "scripts/run.sh"]