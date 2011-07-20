DROP TABLE IF EXISTS `pol`;
CREATE TABLE  `pol` (
  `pol` varchar(4096) DEFAULT NULL,
  `user` varchar(255) DEFAULT NULL,
  `res` varchar(4096) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  KEY `index_pol` (`pol`(767)) USING BTREE,
  KEY `index_user` (`user`) USING BTREE,
  KEY `index_res` (`res`(767)) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=ascii;

--
CREATE TABLE  `xml` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `xml` text NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `FK_xml_1` FOREIGN KEY (`id`) REFERENCES `pol` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- Update 2 Added primary key
-- ALTER TABLE `pol` ADD COLUMN `id` INTEGER UNSIGNED NOT NULL AUTO_INCREMENT AFTER `created`,  ADD PRIMARY KEY (`id`);

-- Update 1
-- ALTER TABLE `pol`.`pol` ADD COLUMN `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `res`;

-- CREATE TABLE  `pol` (
--  `pol` varchar(4096) DEFAULT NULL,
--  `user` varchar(255) DEFAULT NULL,
--  `res` varchar(4096) DEFAULT NULL,
--  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
--  KEY `index_pol` (`pol`(767)) USING BTREE,
--  KEY `index_user` (`user`) USING BTREE,
--  KEY `index_res` (`res`(767)) USING BTREE
-- ) ENGINE=InnoDB DEFAULT CHARSET=ascii;