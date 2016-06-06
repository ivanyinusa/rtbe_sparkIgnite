/**
 * Created by root on 4/13/16.
 */
var jsConsoleDOM = document.createElement('div');
jsConsoleDOM.setAttribute('id', 'jsConsole');
jsConsoleDOM.setAttribute('style', 'bottom:0px;right:0px;position:fixed; z-index: 99;');
var sourceImg = chrome.extension.getURL("page_code.png");
var consoleImg = chrome.extension.getURL("console.png");
var runImg = chrome.extension.getURL("run.png");
var jsConsoleDOMHTML = ['<div id="jsConsole">',
    '<span id="jsConsoleErrors" style="background-color:#fff;color:#f33;"></span>',
    '<input id="jsConsoleShowSourceButton" type="image" title="show source"',
    'src="' + sourceImg + '"',
    'style="float:right;margin:0px 3px;"',
    'onClick="document.getElementById(\'bodyCode\').style.display=\'block\';',
    'document.getElementById(\'jsConsoleShowSourceButton\').style.display=\'none\';',
    'document.getElementById(\'jsConsoleHideSourceButton\').style.display=\'inline\';',
    'document.getElementById(\'bodyCode\').value=document.getElementsByTagName(\'body\')[0].innerHTML;"',
    '/>',
    '<input id="jsConsoleHideSourceButton" type="image" title="hide source"',
    'src="' + sourceImg + '"',
    'style="float:right;display:none;border:1px solid blue;margin:0px 3px;"',
    'onClick="document.getElementById(\'bodyCode\').style.display=\'none\';',
    'document.getElementById(\'jsConsoleHideSourceButton\').style.display=\'none\';',
    'document.getElementById(\'jsConsoleShowSourceButton\').style.display=\'inline\';"',
    '/>',
    '<input id="jsConsoleShowConsoleButton" type="image" title="show console"',
    'src="' + consoleImg + '"',
    'style="float:right;display:none;margin:0px 3px;"',
    'onClick="document.getElementById(\'jsCode\').style.display=\'block\';',
    'document.getElementById(\'jsConsoleShowConsoleButton\').style.display=\'none\';',
    'document.getElementById(\'jsConsoleHideConsoleButton\').style.display=\'inline\';"',
    '/>',
    '<input id="jsConsoleHideConsoleButton" type="image" title="hide console" ',
    'src="' + consoleImg + '"',
    'style="float:right;border:1px solid blue;margin:0px 3px;"',
    'onClick="document.getElementById(\'jsCode\').style.display=\'none\';',
    'document.getElementById(\'jsConsoleHideConsoleButton\').style.display=\'none\';',
    'document.getElementById(\'jsConsoleShowConsoleButton\').style.display=\'inline\';"',
    '/>',
    '<input type="image" title="execute JS" id="runScriptButton"',
    'src="' + runImg + '"',
    'style="float:right;margin:0px 3px;"',
    'onClick="eval(document.getElementById(\'jsInitCode\').value);jsConsole.runScript();"',
    '/><br />',
    '<textarea id="jsCode" rows="5" cols="60" style="float:right;clear:right;"></textarea>',
    '<textarea id="bodyCode" rows="15" cols="60" style="display:none;float:right;clear:right;"></textarea>',
    '</div>',
    '<textarea id="jsInitCode">',
    'alert("translate");',
    'var jsConsole = {',
    'runScript:function(){',
    'try{',
    'eval(document.getElementById(\'jsCode\').value);',
    '}catch(e){',
    'document.getElementById(\'jsConsoleErrors\').innerHTML = e;',
    '}',
    'document.getElementById(\'bodyCode\').value=document.getElementsByTagName(\'body\')[0].innerHTML;',
    'document.getElementById(\'runScriptButton\').onClick="jsConsole.runScript();";',
    '}',
    '}',
    /*'alert("ste");',
     'var blah = {goit:function(){try{return "hello";}catch(e){}}};',
     'alert(blah.goit());',*/
    '</textarea>'];
jsConsoleDOM.innerHTML = jsConsoleDOMHTML.join('');
document.getElementsByTagName('body')[0].appendChild(jsConsoleDOM);
