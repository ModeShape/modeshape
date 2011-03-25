#!/usr/bin/python
import re
import urllib
import htmlentitydefs
try:
    import simplejson as json
except ImportError:
    import json


##
# Removes HTML or XML character references and entities from a text string.
#
# @param text The HTML (or XML) source text.
# @return The plain text, as a Unicode string, if necessary.

def unescape(text):
    def fixup(m):
        text = m.group(0)
        if text[:2] == "&#":
            # character reference
            try:
                if text[:3] == "&#x":
                    return unichr(int(text[3:-1], 16))
                else:
                    return unichr(int(text[2:-1]))
            except ValueError:
                pass
        else:
            # named entity
            try:
                text = unichr(htmlentitydefs.name2codepoint[text[1:-1]])
            except KeyError:
                pass
        return text # leave as is
    return re.sub("&#?\w+;", fixup, text)

class Jira(object):  
  '''Encapsulates JIRA access used when getting issues related to ModeShape release'''
 
  def __init__(self, jira_url, jira_project_key, jira_project_id, project_name, jira_release_version):        
    self.jira_url = jira_url
    self.project_key = jira_project_key
    self.version = jira_release_version
    self.project_info = dict()
    self.project_info['id'] = jira_project_id
    self.project_info['name'] = project_name
    self.version_info = dict()
    self.issues_by_id = dict()
    self.issues_by_email = dict()
    self.issue_types = list()
    self.contributor_roles = list()
    self.fetch = False
    try:
      if settings['verbose']:
        self.verbose = True
    except:
      self.verbose = False
  
  def __fetch_if_needed(self):
    if not self.fetch:
      self.fetch_release_info()
      return True
    return False

  def fetch_release_info(self):
    '''Connects to JIRA and downloads the information for the release'''
    self.__get_release_info_from_jira()
    self.__get_issues_from_jira()
    self.issues_by_email = dict()
    self.__get_contributions_from_jira()
    self.fetch = True

  def get_release_notes_in_markdown(self):
    '''Returns a string containing the Markdown representation of the issues addressed in the release.'''
    self.__fetch_if_needed()
    lines = list()
    for issue_type in self.issue_types:
      lines.append('### %s' % (issue_type) )
      for issue in self.issues_by_id.values():
        if issue['type'] == issue_type:
          lines.append('- [%s][%s] - %s' % (issue['id'],issue['url'],issue['title']) )
      lines.append('')
    return '\n'.join(lines)

  def get_contribution_emails(self):
    '''Returns a dictionary containing the email text keyed by email address for each person that contributed to the release.'''
    email_text_by_address = dict()
    for email,user in self.issues_by_email.items():
      email_text_by_address[email] = self.get_contribution_email_text_for(email)
    return email_text_by_address

  def get_contributor_emails(self):
	  return list(self.issues_by_email.keys())

  def get_contributor_name(self,email_address):
    self.__fetch_if_needed()
    if email_address not in self.issues_by_email:
	  return None;
    user_info = self.issues_by_email[email_address]
    return user_info['lastName']

  def get_contribution_html(self,bcc_address):
    '''Returns the content of a simple HTML page containing mailto links contain the emails to each person that contributed to the release.'''
    lines = list()
    lines.append("<html>");
    lines.append(" <body>");
    lines.append("  <h2>Thank you emails for %s %s contributors</h2>" % (self.project_info['name'],self.version) );
    lines.append("  <p>Click on each link to open an email in your mail application:</p>");
    lines.append("  <ul>");
    subject = "Thank you for your contribution to %s %s" % (self.project_info['name'],self.version)
    bcc = ""
    if bcc_address != None:
      bcc = "&bcc=%s" % (bcc_address)
    for email,user in self.issues_by_email.items():
      body = urllib.quote(self.get_contribution_email_text_for(email))
      mailto = "mailto:%s?%s&subject=%s&body=%s" % (email,bcc,subject,body)
      fullName = self.issues_by_email[email]['name']
      lines.append("<li><a href=\"%s\">%s</a></li>" % (mailto,fullName))
    lines.append("  </ul>");
    lines.append(" </body>");
    lines.append("</html>");
    return "\n".join(lines)

  def get_contribution_email_text_for(self,email_address):
    '''Returns the email text for the person with the supplied email address that contributed to the release, or null if there was no such contributor.'''
    self.__fetch_if_needed()
    if email_address not in self.issues_by_email:
	  return None;
    user_info = self.issues_by_email[email_address]
    text = '''Hello {user},

Thank you for participating in the {project} community. We've recently released {project} {version}, and you've played an important part in this release by helping identify and solve the following issues:

{issue_list}

For more details about {project} and the new {version} release, please see the project website at {project_url}.

We greatly appreciate your contribution as it helped us make {project} better for you as well as for other users. Please keep up the good work, and we encourage you to continue to give us feedback and play an active role in the {project} community.

Thank you very much,
--The {project} team


{issue_links}
'''
    issue_keys = set()
    issue_list = list()
    # Figure out the ordered issues ...
    for role in self.contributor_roles:
      if role in user_info:
        issues_by_id = user_info[role]
        for key in issues_by_id.keys():
          if key not in issue_keys: 
            issue_keys.add(key)
            issue_list.append(self.__sortable_issue_id(key))
    # Sort the list of issues
    issue_list.sort()

    # Generate the list of issues and links ...
    link_counter = 1
    issue_lines = list()
    link_lines = list()
    for issue_number in issue_list:
      isuue_id =  "%s-%s" % (self.project_key,issue_number)	    
      issue = self.issues_by_id[isuue_id]
      url = issue['url']
      title = issue['title']
      key = issue['id']
      issue_lines.append('- %s [%s] - %s' % (key,link_counter,title) )
      link_lines.append('[%s] %s' % (link_counter,url) )
      link_counter = link_counter + 1

    # Format the email text with the variables for this user ...        
    first_name = user_info['firstName']
    proj_name = self.project_info['name']
    proj_url = self.project_info['url']
    version = self.version
    issues = '\n'.join(issue_lines)
    links = '\n'.join(link_lines)
    return text.format(user=first_name,project=proj_name,version=self.version,project_url=proj_url,issue_list=issues,issue_links=links)

  def __get_release_info_from_jira(self):
    request_url = "%sjira/rest/api/2.0.alpha1/project/%s" % (self.jira_url,self.project_key)
    #print "Request URL = %s" % (request_url)
    # make the REST request ...
    socket = urllib.urlopen(request_url)
    json_response = socket.read()
    socket.close()
    # decode the JSON results into a dictionary ...
    data = json.loads(json_response)
    project_key = data['key']
    project_desc = data['description']
    project_url = data['url']
    for key in ['key','description','url']:
      self.project_info[key] = data[key]
    # Find the version information ...
    versions_list = data['versions']
    for version_data in data['versions']:
      version_name = version_data['name']
      if version_name == self.version :
        self_url = version_data['self']
        version_data['id'] = self_url.rpartition('/')[2]
        self.version_info.update(version_data)
        break
    return

  def __sortable_issue_id(self,issue_id):
    exp = re.compile("\-([0-9]+)")
    m = exp.search(issue_id)
    return int(m.group(1))

  def __get_issues_from_jira(self):
    version_id = self.version_info['id']
    project_id = self.project_info['id']
    request_url = "https://issues.jboss.org/secure/ReleaseNote.jspa?projectId=%s&version=%s&styleName=Text" % (project_id,version_id)
    # make the HTML request ...
    socket = urllib.urlopen(request_url)
    html_response = socket.read()
    socket.close()
    # parse the HTML to extract find the relevant lines ...
    issue_type = ''
    issue_exp = re.compile('\[(.*?)\]\s\-\s(.*)$')
    for line in html_response.splitlines():
      line = line.strip(' ')
      if line.startswith('** '):
        issue_type = line.strip('* ')
        self.issue_types.append(issue_type)
      elif line.startswith('* ['):
        m = issue_exp.search(line)
        issue_data = dict()
        issue_id = m.group(1)
        issue_data['id'] = issue_id
        issue_data['title'] = unescape(m.group(2))
        issue_data['url'] = '%sbrowse/%s' % (self.jira_url,issue_id)
        issue_data['type'] = issue_type
        self.issues_by_id[issue_id] = issue_data

  def __get_contributions_from_jira(self):
    version_id = self.version_info['id']
    project_id = self.project_info['id']
    request_url = "%ssecure/ConfigureReport.jspa?versions=%s&ctype=R&ctype=A&ctype=C&ccompany=A&selectedProjectId=%s&reportKey=org.jboss.labs.jira.plugin.patch-contributions-report-plugin:involvedInReleaseReport&Next=Next" % (self.jira_url,version_id,project_id)
    # make the HTML request ...
    socket = urllib.urlopen(request_url)
    html_response = socket.read()
    socket.close()
    # print html_response
    # Parse the HTML to extract the contribution information ...
    contribution_exp = re.compile('\<th\scolspan="2">(.*?)</th>')
    href_exp = re.compile('href=\"(.*?)"')
    td_exp = re.compile('\<td.*?\>(.*?)\<')
    name_exp = re.compile('\<td\>(.*?)\s(.*?)\<')
    issue_exp = re.compile('\<a\shref=\"(.*?)"\>(.*?)\<')
    self.contributor_roles = list()
    lines = html_response.splitlines()
    it = iter(lines)
    try:
        line = it.next()
        while True:
          try:
            line = line.strip(' ')
            if line.startswith('<th colspan'):
              m = contribution_exp.search(line)
              self.contributor_roles.append(m.group(1))
              line = it.next()
            elif line.startswith('<td><a href="'):
              # get the URL for the user's profile ...
              user_info = dict()
              user_info['url'] = href_exp.search(line).group(1)
              # get the name of the user ...
              line = it.next()
              m = name_exp.search(line)
              user_info['firstName'] = m.group(1)
              user_info['lastName'] = m.group(2)
              user_info['name'] = "%s %s" % (user_info['firstName'],user_info['lastName'])
              # get the email address for the user ...
              line = it.next()
              email = td_exp.search(line).group(1)
              user_info['email'] = email
              for role in self.contributor_roles:
                # get the number of issues ...
                line = it.next()
                count = int(td_exp.search(line).group(1))
                # get the opening 'td' tag, which should be on its own line ...
                line = it.next()
                # get the issues, which each should be on their own line ...
                user_issues_for_role = dict()
                for i in range(count):
                  line = it.next()
                  m = issue_exp.search(line)
                  issue = dict()
                  issue['type'] = role
                  issue['url'] = m.group(1)
                  issue['issue'] = m.group(2)
                  user_issues_for_role[issue['issue']] = issue
                # get the closing 'td' tag, which should be on its own line ...
                line = it.next()
                user_info[role] = user_issues_for_role
              # record this user by email address ...
              self.issues_by_email[email] = user_info
              # read the next line
              line = it.next()
            else:
              line = it.next()
          except StopIteration:
            break
    except StopIteration:
      # just continue
      return


### This is the starting place for this script.
def main():
  print "Connecting to JIRA ..."
  jira_url = "https://issues.jboss.org/"
  project_key = 'MODE'
  project_name = 'ModeShape'
  project_id = '12310930'
  version = '2.5.0.Beta1'
  jira = Jira(jira_url,project_key,project_id,project_name,version)
  jira.fetch_release_info()
  #print jira.get_release_notes_in_markdown()
  #print jira.project_info()
  #print jira.version_info()
  #print jira.issues_by_email()
  #print jira.get_contributor_emails()
  print jira.get_contribution_email_text_for('rhauch@jboss.org')
  #print jira.get_contribution_html('rhauch@redhat.com')

if __name__ == "__main__":
  main()  

