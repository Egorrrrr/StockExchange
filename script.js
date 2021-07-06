const IP = "http://localhost:3001";
var market;
var code = null;
var inst;

function makeOrder(e){
  var side = e ? "BUY" : "SELL" ;
  var price = document.getElementById("price").value
  var qty = document.getElementById("qty").value
  
  fetch(IP+"/new-order-single",{
    method: 'POST',
    body:JSON.stringify({code: code, side:side, price:price, qty:qty, instrument:inst})
  })
  .then((response) => {
    

  })
  .then((data) => {
    
  })

}


function login(){
   var name = document.getElementById("loginname").value;
   Cookies.setCookie("name", name);
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

var targetcolor;
function printOrderBook(e){
  if(targetcolor != null){
    targetcolor.style.color = "green"
  }
  targetcolor = e.target
  e.target.style.color = "white";
  var isntname = document.getElementById("inst");
  isntname.innerHTML = "INS:"+ e.target.innerHTML;
  inst = e.target.innerHTML;
  var selltable = document.getElementById("selltab")
  var buytable = document.getElementById("buytab")
  selltable.innerHTML = "";
  buytable.innerHTML = "";
  
  for(const[keysellord, valsellord] of Object.entries(market.instruments[inst].sellOrders)){

    var order = document.createElement("div");
    order.className = "order"

    var id = document.createElement("div");
    id.className = "id"
    id.innerHTML = "#" + keysellord;

    var price = document.createElement("div");
    price.className = "price";
    price.innerHTML = "$" + valsellord.price;

    var qty = document.createElement("div");
    qty.className = "qty";
    qty.innerHTML = valsellord.qty;

    order.appendChild(id);
    order.appendChild(price);
    order.appendChild(qty);
    selltable.appendChild(order)
    
  }
  for(const[keybuyord, valbuyord] of Object.entries(market.instruments[inst].buyOrders)){
    
    var order = document.createElement("div");
    order.className = "order"

    var id = document.createElement("div");
    id.className = "id"
    id.innerHTML = "#" + keybuyord;

    var price = document.createElement("div");
    price.className =  price;
    price.innerHTML = "$" + valbuyord.price;

    var qty = document.createElement("div");
    qty.className = "qty";
    qty.innerHTML = valbuyord.qty;

    order.appendChild(id);
    order.appendChild(price);
    order.appendChild(qty);
    buytable.appendChild(order)
  }



}

function restoreTable(){
  var table = document.getElementById("tableone");
  table.innerHTML = "";

  var instheader = document.createElement("th");
  var sellheader = document.createElement("th");
  var buyheader = document.createElement("th");
  instheader.innerHTML = "INSTRUMENT"
  sellheader.innerHTML = "SELL"
  buyheader.innerHTML = "BUY"
  table.append(instheader)
  table.append(sellheader)  
  table.append(buyheader)  
}

if(document.location.toString().includes("index")){

    var flag = true
    if(flag){
      var msgEvent = new EventSource(IP + "/sse")

      msgEvent.addEventListener("connected", msg =>{
          code = msg.data;
          var name = Cookies.getCookie("name");
          var req = new XMLHttpRequest();
          req.open("POST", IP + "/verify");
          req.send(JSON.stringify({name:name, code:code}));
      });
      msgEvent.addEventListener("marketSnapshot", msg =>{

        var marketSnapshot = JSON.parse(msg.data)
        market = marketSnapshot;
        restoreTable();
        for (const [key, value] of Object.entries(marketSnapshot.instruments)) {
          console.log(key, value);

          
          if(inst == key){
            var selltable = document.getElementById("selltab")
            var buytable = document.getElementById("buytab")
            selltable.innerHTML = "";
            buytable.innerHTML = "";
            
            for(const[keysellord, valsellord] of Object.entries(value.sellOrders)){

              var order = document.createElement("div");
              order.className = "order"

              var id = document.createElement("div");
              id.className = "id"
              id.innerHTML = "#" + keysellord;

              var price = document.createElement("div");
              price.className = "price";
              price.innerHTML = "$" + valsellord.price;

              var qty = document.createElement("div");
              qty.className = "qty";
              qty.innerHTML = valsellord.qty;

              order.appendChild(id);
              order.appendChild(price);
              order.appendChild(qty);
              selltable.appendChild(order)
              
            }
            for(const[keybuyord, valbuyord] of Object.entries(value.buyOrders)){
              
              var order = document.createElement("div");
              order.className = "order"

              var id = document.createElement("div");
              id.className = "id"
              id.innerHTML = "#" + keybuyord;

              var price = document.createElement("div");
              price.className = "price";
              price.innerHTML = "$" + valbuyord.price;

              var qty = document.createElement("div");
              qty.className = "qty";
              qty.innerHTML = valbuyord.qty;

              order.appendChild(id);
              order.appendChild(price);
              order.appendChild(qty);
              buytable.appendChild(order)
            }
          }

          var table = document.getElementById("tableone");

          var instheader = document.createElement("th");
          var sellheader = document.createElement("th");
          var buyheader = document.createElement("th");
          table.append  
          var tr = document.createElement("tr");
          var td = document.createElement("td");
          td.innerHTML = key;
          td.onclick = printOrderBook;
          tr.appendChild(td);
          table.appendChild(tr);
        }        
    });
    flag= false
  }
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
