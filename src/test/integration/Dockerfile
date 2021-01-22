FROM python:2-onbuild

# Set timezone
ENV TZ=America/Los_Angeles
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

CMD [ "python", "./integrationtests.py", "-v", "-i", "configuration.json" ]

USER nobody:nogroup
