import urllib
import os
rootDir = os.path.dirname(os.path.dirname(os.path.realpath(__file__)))
urllib.urlretrieve('http://35.166.131.3/apiconnection/all.php',rootDir+'/src/main/resources/conf/authorized-ip-mac');