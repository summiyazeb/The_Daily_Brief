
import os
from dotenv import load_dotenv
from neo4j import GraphDatabase

load_dotenv()

URI = os.getenv("NEO4J_URI")
USERNAME = os.getenv("NEO4J_USERNAME")
PASSWORD = os.getenv("NEO4J_PASSWORD")

driver = GraphDatabase.driver(
    URI,
    auth=(USERNAME, PASSWORD)
)

def test_connection():
    driver.verify_connectivity()
    print("Successfully connected to Neo4j AuraDB!")

if __name__ == "__main__":
    test_connection()
    driver.close()
