"""Original geometric Possessory artwork, authored in code. No external artwork."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'assets/brand';OUT.mkdir(parents=True,exist_ok=True)
NAVY='#24344A';IVORY='#F8F6F1';SAGE='#6D8B78';GOLD='#C5A46D'
def icon(size=2048):
    image=Image.new('RGB',(size,size),NAVY);d=ImageDraw.Draw(image)
    scale=size/108
    def points(coords):return [(x*scale,y*scale) for x,y in coords]
    d.line(points([(27,49),(54,27),(81,49),(81,80),(27,80),(27,49)]),fill=IVORY,width=round(4*scale),joint='curve')
    d.rectangle((43*scale,55*scale,65*scale,71*scale),fill=GOLD)
    d.line(points([(49,62),(53,66),(60,59)]),fill=NAVY,width=round(2.5*scale),joint='curve')
    image.save(OUT/f'icon-master-{size}.png')
    for n in [512,114]:image.resize((n,n),Image.Resampling.LANCZOS).save(OUT/f'icon-{n}.png')

def svg(name,body,dark=False):
    bg=NAVY if dark else IVORY;line=IVORY if dark else NAVY
    (OUT/f'{name}{"-dark" if dark else ""}.svg').write_text(f'<svg xmlns="http://www.w3.org/2000/svg" width="800" height="600" viewBox="0 0 800 600"><rect width="800" height="600" rx="40" fill="{bg}"/><circle cx="400" cy="300" r="210" fill="{SAGE}" opacity=".14"/><g stroke="{line}" stroke-width="8" stroke-linecap="round" stroke-linejoin="round">{body}</g></svg>',encoding='utf-8')

def main():
    icon()
    banner=Image.new('RGB',(1024,500),NAVY)
    draw=ImageDraw.Draw(banner)
    font=ImageFont.truetype('C:/Windows/Fonts/georgia.ttf',76)
    draw.text((70,196),'possessory',font=font,fill=IVORY)
    draw.line((72,298,195,298),fill=GOLD,width=4)
    mark=Image.open(OUT/'icon-master-2048.png').resize((390,390),Image.Resampling.LANCZOS)
    banner.paste(mark,(580,55))
    banner.save(OUT/'promotional-banner-1024x500.png')
    scenes={
      'onboarding':'<path d="M210 270L400 110 590 270V490H210Z" fill="none"/><rect x="290" y="290" width="220" height="150" rx="15" fill="#C5A46D"/><path d="M350 360L390 400 460 325" fill="none"/>',
      'receipt-scanner':'<rect x="260" y="110" width="280" height="380" rx="20" fill="none"/><path d="M310 200H490M310 260H450M310 320H490M310 420H490"/><path d="M190 260V220H230M610 260V220H570M190 360V400H230M610 360V400H570" stroke="#6D8B78"/>',
      'warranty-tracker':'<path d="M400 100L570 170V300Q570 430 400 500Q230 430 230 300V170Z" fill="none"/><path d="M310 290L375 355 490 230" stroke="#6D8B78" fill="none"/>',
      'maintenance':'<circle cx="400" cy="300" r="160" fill="none"/><path d="M400 190V300L480 350" fill="none"/><circle cx="525" cy="425" r="65" fill="#C5A46D"/><path d="M495 425L515 445 555 405" fill="none"/>',
      'report-export':'<path d="M260 100H470L550 180V500H260Z" fill="none"/><path d="M470 100V180H550M310 250H490M310 310H450M310 370H490" fill="none"/><path d="M430 430H630M580 380L630 430 580 480" stroke="#6D8B78" fill="none"/>',
      'premium':'<path d="M250 210L310 150H490L550 210 400 450Z" fill="#C5A46D"/><path d="M250 210H550M310 150L350 210 400 450 450 210 490 150" fill="none"/>',
      'inventory-placeholder':'<path d="M240 210L400 130 560 210V400L400 480 240 400Z" fill="none"/><path d="M240 210L400 290 560 210M400 290V480M320 170L480 250" fill="none"/>',
      'room-living':'<rect x="210" y="240" width="380" height="180" rx="35" fill="#6D8B78"/><path d="M210 320H590M270 240V320M530 240V320M240 420V455M560 420V455" fill="none"/>',
      'room-kitchen':'<rect x="240" y="120" width="320" height="380" rx="25" fill="none"/><path d="M240 260H560M285 180V220M285 300V360" fill="none"/>',
      'room-bedroom':'<path d="M190 440V250Q190 210 230 210H570Q610 210 610 250V440M190 390H610M190 330H610" fill="none"/><rect x="240" y="250" width="120" height="70" rx="12" fill="#C5A46D"/><rect x="440" y="250" width="120" height="70" rx="12" fill="#C5A46D"/>',
      'room-office':'<path d="M190 350H610M230 350V480M570 350V480" fill="none"/><rect x="280" y="140" width="240" height="150" rx="15" fill="none"/><path d="M400 290V350M340 350H460" fill="none"/>',
      'room-garden':'<path d="M400 490V220M400 340Q230 340 230 180Q400 180 400 340M400 270Q570 270 570 110Q400 110 400 270" fill="#6D8B78"/>',
      'room-storage':'<rect x="230" y="310" width="340" height="180" rx="10" fill="none"/><rect x="290" y="130" width="220" height="180" rx="10" fill="#C5A46D"/><path d="M360 370H440M370 190H430" fill="none"/>'
    }
    for name,body in scenes.items():
        svg(name,body);svg(name,body,True)
    print(f'{len(list(OUT.iterdir()))} original asset files')
if __name__=='__main__':main()
