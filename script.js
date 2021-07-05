const IP = "localhost:3001";
var code = null;

function login(){
   var name = document.getElementById("loginname").value;
   
   var pass = document.getElementById("pass").value;
   fetch(IP+"/login",{
    method: 'POST',
    body:JSON.stringify({username:name})
  })
  .then((response) => {
    window.location.pathname = '/index.html'

  })
  .then((data) => {
    
  })
  
    
}

if(document.location.toString().includes("chat")){

    var msgEvent = new EventSource(Cookies.getCookie("Ip") + "/sse")

    msgEvent.addEventListener("connected", msg =>{
        code = msg.data;
        var req = new XMLHttpRequest();
        req.open("POST", IP + "/check-in");
        req.send(code);
    });
}







class Cookies {

    static setCookie(name, value, options = {}) {
      options = {
        path: '/',
        // при необходимости добавьте другие значения по умолчанию
        ...options
      };
    
      if (options.expires instanceof Date) {
        options.expires = options.expires.toUTCString()
      }
      let updatedCookie = encodeURIComponent(name) + "=" + encodeURIComponent(value)
    
      for (let optionKey in options) {
        updatedCookie += "; " + optionKey
        let optionValue = options[optionKey]
    
        if (optionValue != true) {
          updatedCookie += "=" + optionValue
        }
      }
      document.cookie = updatedCookie
    }
    
    static getCookie(name) {
      let matches = document.cookie.match(new RegExp(
        "(?:^|; )" + name.replace(/([\.$?*|{}\(\)\[\]\\\/\+^])/g, '\\$1') + "=([^;]*)"
      ))
      return matches ? decodeURIComponent(matches[1]) : undefined
    }
    
    static deleteAllCookies() {
      if(document.cookie != null){
      var cookies = document.cookie.split(";")
      for (var i = 0; i < cookies.length; i++) {
          var cookie = cookies[i]
          var eqPos = cookie.indexOf("=")
          var name = eqPos > -1 ? cookie.substr(0, eqPos) : cookie
          document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT"
        }
    }
}
}
