DROP TABLE IF EXISTS `pol`;
CREATE TABLE  `pol` (
  `pol` varchar(4096) DEFAULT NULL,
  `user` varchar(255) DEFAULT NULL,
  `res` varchar(4096) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `index_pol` (`pol`(767)) USING BTREE,
  KEY `index_user` (`user`) USING BTREE,
  KEY `index_res` (`res`(767)) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=ascii;

-- Update
-- ALTER TABLE `pol`.`pol` ADD COLUMN `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `res`;

DROP TABLE IF EXISTS `pol`.`pol`;
CREATE TABLE  `pol`.`pol` (
  `pol` varchar(4096) DEFAULT NULL,
  `user` varchar(255) DEFAULT NULL,
  `res` varchar(4096) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `index_pol` (`pol`(767)) USING BTREE,
  KEY `index_user` (`user`) USING BTREE,
  KEY `index_res` (`res`(767)) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=ascii;