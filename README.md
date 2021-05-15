# WikiSteps
[Demo](https://youtu.be/AP7DuDFvzWY)

Android app, main function: to find shortest path between two Wikipedia articles.

The app shows how many steps between articles, how long the search took, how many pages were visited, how many titles were checked and the actual path with steps. User can visit any step to get short description and main image of each page in the path.

Multilanguage support with FTS table. 
#### Search Algorithm
App used Efficient Two-End Breadth First Search algorithm. Efficient means that BFS from start to goal and from goal to start are doing step-by-step. At each step, the queues are compared to select the smaller one for next iteration. 

This is a huge improvement over the simple BFS algorithm. The cost of parsing Wikipedia page is about half of second (MediaWiki API only gets 500 links per page, so - it can be more than one request per page). The simple BFS algorithm is very slow as you need to visit thousands of pages. Two-end BFS find the path in most cases, visited less than ten pages.  
 
The algorithm has "two" parts. One in the SearchActivity class to compare queues and activate parsing for each iteration until find any match(es). Second in the StepService class: compare titles with previous ones when new titles from API (with volley library) are stored. 

Even if look for the shortest path (its depending on the number of visited pages), quite often it will be possible to go from one article to another in different ways. That's why the choice to show the result fell on nested RecyclerView. 

A parallel thread in SearchActivity processing checked titles to show this titles while main algorithm was looking for a path.

  
#### Show results
The result can store unpredictable paths and steps. In most cases it should be stored on one screen: paths and steps. Cases that contain more names to display - RecyclerView performs the task. As a result, scrolling works in both dimensions: vertical and horizontal. For the nested RecyclerView in PathActivity it is necessary to create new objects: Steps and Paths. And two adapters: for inner and outer RecyclerView.
#### Language FTS 
All wikipedia codes and languages are stored into full text search table from local JSON file at the first start of the app. 
#### Preferences
The app stores two values: the language code of the previous session and the boolean first start (which displays that the default language is English). 

Language code is used in different activities to retrieve data from the MediaWiki API (for suggestions, finding path, and displaying a short description with image on a single page). 
#### Corner cases
Wikipedia is case-sensitive. AutoComplete suggestions help the user use existing Wikipedia articles as input. 

Another case is to check whether input has links to other articles (is it start) or input has "links here" (is it goal). This is a check on the second iteration for each entry after parsing the links for each page. If there are no links - return to mainActivity and show the message to the user to try another input.
####



  







