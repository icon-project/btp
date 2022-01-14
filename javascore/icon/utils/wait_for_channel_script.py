import sys
import requests
import backoff
import json

@backoff.on_exception(backoff.expo,
                      (requests.exceptions.Timeout,
                       requests.exceptions.ConnectionError),
                       max_time=float(sys.argv[3]))
def get_url(url):
    return requests.get(url)

while True:
    response = get_url(sys.argv[1] + "/" + sys.argv[2]).json()
    if len(response) > 0:
        print(response[0]["nid"])
        break