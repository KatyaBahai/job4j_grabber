CREATE TABLE post (
id serial primary key,
title text,
link text UNIQUE,
created timestamp,
description text
);