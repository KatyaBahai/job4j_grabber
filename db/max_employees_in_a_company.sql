CREATE TABLE company
(
    id integer NOT NULL,
    name character varying,
    CONSTRAINT company_pkey PRIMARY KEY (id)
);

CREATE TABLE person
(
    id integer NOT NULL,
    name character varying,
    company_id integer references company(id),
    CONSTRAINT person_pkey PRIMARY KEY (id)
);


INSERT INTO company(id, name) VALUES(1, 'LSclinic'), (2, 'Magnit'), 
(3, 'UrboCoffee'), (4, 'Doctor Pepper'), (5, 'Bahai school');

INSERT INTO person(id, name, company_id) VALUES(1, 'James Bond', 1), (2, 'Angelica Varum', 1), (3, 'Michael Jackson', 1), 
(4, 'Hillary Clinton', 2), (5, 'Orlando Bloom', 2), (6, 'Leonardo DiCaprio', 3), (7, 'Whitney Houston', 4), 
(8, 'Antony Jones', 4), (9, 'Jennifer Aniston', 5), (10, 'Ariana Grande', 5), (11, 'Sofia Rotaru', 5);


SELECT p.name employer, c.name company
FROM person p LEFT JOIN company c 
ON p.company_id = c.id
WHERE p.company_id != 5; 



SELECT innerCount.com_name_inner, innerCount.num
FROM (SELECT c.name AS com_name_inner, count(p.company_id) AS num 
FROM company c LEFT JOIN person p
ON p.company_id = c.id
GROUP BY c.name
ORDER BY num DESC) innerCount
GROUP BY innerCount.com_name_inner, innerCount.num
HAVING innerCount.num = (SELECT MAX(innerMax.num)
from (SELECT c.name com_name_inner, count(p.company_id) num FROM company c LEFT JOIN person p
ON p.company_id = c.id
GROUP BY c.name
ORDER BY num DESC) innerMax);
