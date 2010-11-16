//
// expand.js -- JavaScript source for expandable content boxes
//
// Author: Brian McCord

/**
 * This script allows for expandable examples and other content boxes.
 *
 * @author bmccord
 */

function toggle_visibility(id) {
       var e = document.getElementById(id);
       if(e.style.display == 'block')
          e.style.display = 'none';
       else
          e.style.display = 'block';
    }

