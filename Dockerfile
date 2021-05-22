# base image
FROM nunopreguica/sd2021tpbase

# working directory inside docker image
WORKDIR /home/sd

# copy the jar created by assembly to the docker image
COPY target/*jar-with-dependencies.jar sd2021.jar

# copy the file of properties to the docker image
COPY trab.props trab.props

# copy keystore
COPY server.ks server.ks

# copy truststore
COPY truststore.ks truststore.ks

# run the server when starting the docker image
CMD ["java", "-cp", "/home/sd/sd2021.jar",
"-Djavax.net.ssl.keyStore=server.ks",
"-Djavax.net.ssl.keyStorePassword=password",
"-Djavax.net.ssl.trustStore=truststore.ks",
"-Djavax.net.ssl.trustStorePassword=changeit",
"sd2021.aula2.server.UserServer"]

