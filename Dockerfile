# base image
FROM nunopreguica/sd2021tpbase

# working directory inside docker image
WORKDIR /home/sd

# copy the jar created by assembly to the docker image
COPY target/*jar-with-dependencies.jar sd2021.jar

# copy the file of properties to the docker image
COPY trab.props trab.props

# copy keystore
COPY users.ks users.ks
COPY sheets.ks sheets.ks

# copy truststore
COPY truststore.ks truststore.ks
