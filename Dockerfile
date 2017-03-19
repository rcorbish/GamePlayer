
FROM openjdk:8

WORKDIR /GamePlayer

ADD build.gradle 					build.gradle
ADD settings.gradle 				settings.gradle
ADD src/main/java					/GamePlayer/src/main/java
ADD src/main/resources/audio		/GamePlayer/src/main/resources/audio
ADD src/main/resources/js			/GamePlayer/src/main/resources/js
ADD src/main/resources/textures		/GamePlayer/src/main/resources/textures
ADD src/main/resources/index.html	/GamePlayer/src/main/resources/index.html

ENV CP build/classes/main

RUN wget -q https://services.gradle.org/distributions/gradle-3.4.1-bin.zip && \
	mkdir /opt/gradle && \
	mkdir jars && \
	unzip -q -d /opt/gradle gradle-3.4.1-bin.zip && \
	ln -s /opt/gradle/gradle-3.4.1/bin/gradle /bin && \
	gradle build && \
	gradle copyDepJars && \
	echo 'for i in jars/*; do CP=$CP:$i; done' > run.sh && \
	echo 'java -cp $CP com.rc.WebServer' >> run.sh && \
	chmod 0700 run.sh   
	

VOLUME [ "/GamePlayer/src/main/resources/data" ]

CMD [ "sh", "/GamePlayer/run.sh" ]  
