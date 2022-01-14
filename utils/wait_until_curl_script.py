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

response = get_url(sys.argv[1] + "/" + sys.argv[2]).json()
# response.update({"node": sys.argv[1]})

print(sys.argv[1])