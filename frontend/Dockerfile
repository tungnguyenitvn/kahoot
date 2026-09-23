FROM node:24.15.0-bookworm-slim
WORKDIR /app
COPY docker-entrypoint.sh /usr/local/bin/quiz-frontend
RUN chmod +x /usr/local/bin/quiz-frontend
ENTRYPOINT ["quiz-frontend"]
CMD ["npm", "start"]
