# -*- coding: utf-8 -*-
import psycopg2
import random
import string
import os
import logging
from datetime import datetime

# ��������� �����������
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

try:
    # ����������� � ���� ������
    conn = psycopg2.connect(
        host=os.getenv("POSTGRES_HOST", "localhost"),
        port=5432,
        dbname=os.getenv("POSTGRES_DB"),
        user=os.getenv("POSTGRES_USER"),
        password=os.getenv("POSTGRES_PASSWORD")
    )
    cur = conn.cursor()

    # ��������� ��������� ������ ������������
    name = "User_" + ''.join(random.choices(string.ascii_letters, k=5))
    email = name.lower() + "@example.com"

    logging.info(f"Creating new user: {name}, email: {email}")
    start_time = datetime.now()

    # ������� ������������ � ��������� ������������ ID
    cur.execute(
        "INSERT INTO users (name, email) VALUES (%s, %s) RETURNING id",
        (name, email)
    )
    user_id = cur.fetchone()[0]
    end_time = datetime.now()

    logging.info(f"User with ID {user_id} created in {end_time - start_time}")

    # ��������� ��������� ������ ������
    product_name = "Product_" + str(random.randint(1, 50))
    quantity = random.randint(1, 10)

    logging.info(f"Creating order for user ID {user_id}: product '{product_name}', quantity {quantity}")
    start_time = datetime.now()

    # ������� ������
    cur.execute(
        "INSERT INTO orders (user_id, product_name, quantity) VALUES (%s, %s, %s)",
        (user_id, product_name, quantity)
    )

    end_time = datetime.now()
    logging.info(f"Order for user ID {user_id} created in {end_time - start_time}")

    # ������������� ����������
    conn.commit()

except Exception as e:
    logging.error(f"An error occurred: {e}")

finally:
    # �������� ����������
    if 'cur' in locals():
        cur.close()
    if 'conn' in locals():
        conn.close()
    logging.info("Update complete.")
