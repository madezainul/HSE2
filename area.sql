-- MySQL dump 10.13  Distrib 8.0.40, for Win64 (x86_64)
--
-- Host: localhost    Database: hse_dev
-- ------------------------------------------------------
-- Server version	8.0.40

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `areas`
--

DROP TABLE IF EXISTS `areas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `areas` (
  `id` varchar(22) NOT NULL,
  `code` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `status` enum('ACTIVE','INACTIVE','UNDER_MAINTENANCE') NOT NULL,
  `responsible_person` varchar(22) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKera7pyn95nh7lsxoirp7nm2ov` (`code`),
  KEY `FK_areas_responsible_person` (`responsible_person`),
  CONSTRAINT `FK_areas_responsible_person` FOREIGN KEY (`responsible_person`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `areas`
--

LOCK TABLES `areas` WRITE;
/*!40000 ALTER TABLE `areas` DISABLE KEYS */;
INSERT INTO `areas` VALUES ('05i8z6CYLTjWJm0tnkH5qv','D-010','','Director Office','ACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('0dIiGtjfx4VvKlHGbsxCNd','C-014','-','Pre-Bending Power Pack Room','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('0G0UWj70GFkgbZImDnxDdt','B-002','-','Visual Inspection','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('0Im4RRL6K46buWtDF3efIW','B-019','-','Marking','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('0P2pcTtdx2Q4CpJEKdZzrI','D-028','','File Room','ACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('0sClJG5g67sRpuherwCsTr','D-004','','Purchase Department Office','ACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('0UsqwK01Yz1MV7mh15os7n','B-007','-','End Facing','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('0VX7X8MIA1JMQj4Vbfi27r','C-007','Under Construction','Kitchen 1','INACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('0Zps8WWYguBCrNh2L9ihtD','D-024','','Metallographic Hardness Room','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('0zZL5E2onfb6jXDDcHdYH2','C-005','Muslim Pray room','Mosque Line - 1','ACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('10gOikvaazy8TmFzcDC1LM','D-017','','Female Kitchen','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('15QleCeLd5RduvzuwJYRGf','A-003',NULL,'Plate Washing','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('1cCikkZ0wTp7TEecuKnnOe','A-004',NULL,'Plate Ultrasonic','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('1fkLumqr3BW3r5BIvqmLm6','A-006',NULL,'Edge Crimping','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('1FPxQusfkaMvsP3fTudZ0a','D-020','','Bend Test Room','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('1lw8MYXvBSDxWdPf3NQPPC','D-018','','Female Toilet','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('1N9f7GtXNSUFsvMeQh71vT','C-022','','Logistic','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('1Na1kPatVD5MZn1d2CR3km','D-016','','Grinding Room (Lab)','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('1OpOaAeQVYInFw4YdiHMQE','B-018','-','Final Inspection','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('1pjLlnxfiir1FHY5jHxdl7','C-001','Empty office in second floor','Empty Office at 2nd Floor','INACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('1VqOyt8lKqFRANIDi22qz9','C-009','','Public Toilet 2 Line - 1','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('1zLNXo3RVp4LKMMyM11zHB','D-021','','Tensile Test Room','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('258VuCnGdcUzH1YYsUVT2z','A-011','','Internal Welding','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('25ZmpjxJiwstb6C8cAlAXb','A-002',NULL,'Tab Welding','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('2B45DSOeVJkTYjpyOpMoj0','A-005',NULL,'Edge Milling','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('2JzL445HbHlSjQBogaj5TZ','C-010','','Maintenance Warehouse','ACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('2PIeRzdI5cEuRe0brHtm7m','B-014','-','Manual Ultrasonic - 2','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('2Q4Y2xVh52uU1y1K5NpbjW','C-016','-','Generator Room Line - 1','ACTIVE','08VWazKiodXJZAhbPE9ODc'),('2RTIDiDUQwEVfAI3nktCKW','D-022','','Drop-Weight Tear Test Room','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('2WAU7Rbth0oOniJOZsDjqI','C-021','-','Quality Control Office','ACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('2wsWqhUaZlXG9h29xBAom3','D-041','','Public Toilet 2 Line-1','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('2ZDIR5rHaazpMtMorXK0Km','D-030','-','Maintenance Workshop','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('39gZ1lirHoD1cFZbPoqRIn','D-015','','Chemical Composition Room','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('3A1SpqlruqVzB23EaNjHeO','A-013','','External Welding','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('3AwCZIICMSUeJSsOoZTedA','B-012','-','Sampling','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('3cEcS0a8zA322Y04j3DERz','D-019','','Impact Charpy Test Room','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('3CFAOgrF5kUwly8Mdc3jau','B-013','-','Automatic Ultrasonic - 2','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('3cv71tuugHcZ61hgYkFLfo','E-002','','Forklift','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('3cZQBhCn9UhKYOwOY6VdS7','C-011','-','Compressor Room','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('3e53J6m37YFi6KACU23bl6','D-005','','HR Department Office','ACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('3hAcMcsTOhvFtYIeDjdGY7','B-020','','Pipe Straightening','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('3Lwn4bpWTDTCD1LftHAuC5','C-003','Special Room for Training new Recruitment or for Training purpose','Training Room','ACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('3o7YR3xYeTr8ZA6w4URSyo','D-043','','HIC','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('3rLpqL14FKluVDABZkysTu','E-001','','Inside Plant Crane','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('3TNbbwTf35xofZY6Jaq1K4','B-015','-','Radiography Testing - 2','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('3ULItFNS3UMIBdxijULRJw','A-001',NULL,'Feeding','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('3X5Ke0VZq8LfPi5l4tVqmO','D-040','-','Workshop','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('3y7KAVGiANQcew1rfXFmnh','A-007',NULL,'Press Bending','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('3zDJ5maAqY1XUokSo7RwaX','C-020','-','Production Office','ACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('44ZmLq3jCJcGbvpimW50CV','B-005','-','Radiography Testing - 1','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('4IYUVVSAS7AaeJEYTbYsFT','D-002','','Business Development Manager Office','ACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('4MKIeztcgfUaYbSuip6hkB','C-019','-','Maintenance Office','ACTIVE','552XzjDHXutQLARuxN0eZt'),('4MvElS7iwE7ZiPP0wbypoU','C-006','-','Pakistan & India Canteen','ACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('4WaVaWbcPSYFra9R2zmjjV','C-015','-','Diesel Tank','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('50qLnx6goQ7hwUJdCneed9','B-006','-','Repair','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('51TzmBT4fgQa0WLlCPh02D','A-008',NULL,'Pipe washing','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('5H3shpHkt1gt96lcThtMiJ','D-009','','Finance Department Office','ACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('5HP7y7UwfadFJnwZBFZy4h','D-023','','Carbon Sulfur Analyzer Room','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('5kDr7dUYVQV75KJLTW3lTs','B-001','-','Tab Removal','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('5liQogTxio58mQTOHc1tCM','C-004','Chinese and Philipine Canteen','Chinese & Philipine Canteen','ACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('5LvwWBSZyd4oEgDYP9o4bH','E-003','','Outside Crane','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('5mD2m2q9MPLgNYI1DqHDrM','C-017','-','Flux Warehouse','ACTIVE','552XzjDHXutQLARuxN0eZt'),('5ntknK8NatclhrkVHsmSvC','D-003','','Technical Department Office','ACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('5qlCEWw0w4J0KEv2nk7Oal','D-008','','Toilet Admin','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('5ZgGURRIIW9C40YoXBi7xK','B-011','-','Hydrostatic','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('617wZ3ocVRbYKZ1ZvP9xC1','C-002','Public Toilet for normal worker adjacent to line-1','Public Toilet 1 Line-1','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('6blKfeRFOIaX69czn4YD10','D-011','','Sales Department Office','ACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('6Bvar8WE7POBK3R4c08vQV','A-009',NULL,'Tack Welding','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('6eBR7Lt8kzfy3Jxgv30oEl','B-010','-','Pipe Grinding','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('6fw3S7gwmXjZSmqDrNoUX6','D-042','','Security Gate  2','ACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('6HS6kSqRW3zf1GIF0hlVcd','B-008','-','Expansion','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('6jlJ9kC73pu0xLaruN7wRX','B-003','-','Automatic Ultrasonic - 1','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('6KI2SNfwJSUcjjTigihHrB','B-009','','Pipe Sizing','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('6kw5tipEHTw5U95TQ9ICBv','B-017','-','Magnetic Test Particle','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('6l1mmWejbJxkux1WC2zgr1','A-010',NULL,'Tack Repair','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('6OCkaANsJhcTkOwcqgZ7eD','E-005','','Generator Room (Line 1 and 2)','ACTIVE','08VWazKiodXJZAhbPE9ODc'),('6T08gj9LN4fuWKZ64fIv23','A-012','','Slag Cleaning','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('6UNWrRNrEyqrL9EBpOTtom','C-013','-','R.O & Firefighting','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('7bhCLBQTk7wDrj01SyFEKs','B-004','-','Manual Ultrasonic - 1','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('7cA3yK4YKFpRh42WXeMymO','B-016','-','Beveling','ACTIVE','3Bhofh1u4DS7H5ONIeWMLd'),('7e4SOginsptytAVdfdApLU','C-008','-','Saudi Canteen','ACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('7efTH2SPM1j3iU5HQe1SXu','D-012','','Laboratory Office','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('7ez0M03Wk1cizoxYLwHVAK','C-012','-','Cooling Tower','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('7FZd9cEKbs62HWe2tnXQeY','D-013','','Female Office','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('7GzUozqQEEOtMqLprtJt4u','E-004','','Mobile Crane','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('7i01EL9VlFIa66V9gwAbvJ','D-001','','IT Office','ACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('7MtW0h14GtfPNRpQIpWKTk','D-007','','Kitchen HR','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('7NMUOuF9DxRKgZJKi3BoHR','D-006','','Meeting Room HR','ACTIVE','2JdXytpgHr4mzNlZDlpT7P'),('7OFShBVuQGKc43nhuPkTN5','D-025','','Metering Room','ACTIVE','1eh9dj8sr8firI1fMKA8lr'),('7SrJRurmvOM4g1hmzGIEV8','C-018','-','Flux Recovery Room','ACTIVE','4EqEiyRwqVNadx5VqoKWfV'),('7ZPsMIcOpg0Qc8f3S100xn','D-014','','Lab Meeting Room','ACTIVE','4EqEiyRwqVNadx5VqoKWfV');
/*!40000 ALTER TABLE `areas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'hse_dev'
--

--
-- Dumping routines for database 'hse_dev'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-21 13:08:37
