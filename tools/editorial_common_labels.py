"""Explicit AI corrections for observed mistranslations in release catalogues."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

# Columns follow KEYS. Regional variants share only terminology appropriate to both.
KEYS = 'condition_fair condition_repair status_disposed total_items room expired crop_receipt crop_left crop_top crop_right crop_bottom receipt_currency'.split()
ROWS = {
    'es-ES es-419': 'Aceptable|Necesita reparación|Desechado|Pertenencias registradas|Habitación|Vencida|Recortar recibo|Recortar desde la izquierda (%)|Recortar desde arriba (%)|Recortar desde la derecha (%)|Recortar desde abajo (%)|Moneda del recibo',
    'fr-FR fr-CA': 'État moyen|À réparer|Mis au rebut|Biens enregistrés|Pièce|Expirée|Recadrer le reçu|Rogner à gauche (%)|Rogner en haut (%)|Rogner à droite (%)|Rogner en bas (%)|Devise du reçu',
    'de-DE': 'Gebraucht|Reparatur erforderlich|Entsorgt|Erfasste Gegenstände|Raum|Abgelaufen|Beleg zuschneiden|Links zuschneiden (%)|Oben zuschneiden (%)|Rechts zuschneiden (%)|Unten zuschneiden (%)|Währung des Belegs',
    'it-IT': 'Discreto|Da riparare|Smaltito|Beni registrati|Stanza|Scaduta|Ritaglia ricevuta|Ritaglia da sinistra (%)|Ritaglia dall’alto (%)|Ritaglia da destra (%)|Ritaglia dal basso (%)|Valuta della ricevuta',
    'pt-BR': 'Regular|Precisa de reparo|Descartado|Bens cadastrados|Cômodo|Expirada|Recortar recibo|Recortar da esquerda (%)|Recortar de cima (%)|Recortar da direita (%)|Recortar de baixo (%)|Moeda do recibo',
    'pt-PT': 'Razoável|Precisa de reparação|Descartado|Bens registados|Divisão|Expirada|Recortar recibo|Recortar da esquerda (%)|Recortar de cima (%)|Recortar da direita (%)|Recortar de baixo (%)|Moeda do recibo',
    'nl-NL': 'Redelijk|Reparatie nodig|Weggegooid|Geregistreerde spullen|Ruimte|Verlopen|Bon bijsnijden|Links bijsnijden (%)|Boven bijsnijden (%)|Rechts bijsnijden (%)|Onder bijsnijden (%)|Valuta van de bon',
    'pl-PL': 'Dostateczny|Wymaga naprawy|Wyrzucony|Zapisane przedmioty|Pokój|Wygasła|Przytnij paragon|Przytnij z lewej (%)|Przytnij od góry (%)|Przytnij z prawej (%)|Przytnij od dołu (%)|Waluta paragonu',
    'sv-SE': 'Godtagbart|Behöver repareras|Kasserad|Registrerade föremål|Rum|Utgången|Beskär kvitto|Beskär från vänster (%)|Beskär uppifrån (%)|Beskär från höger (%)|Beskär nedifrån (%)|Kvittots valuta',
    'tr-TR': 'Orta|Onarım gerekiyor|Elden çıkarıldı|Kaydedilen eşyalar|Oda|Süresi doldu|Makbuzu kırp|Soldan kırp (%)|Üstten kırp (%)|Sağdan kırp (%)|Alttan kırp (%)|Makbuz para birimi',
    'id-ID': 'Cukup baik|Perlu perbaikan|Dibuang|Barang tercatat|Ruangan|Kedaluwarsa|Pangkas tanda terima|Pangkas dari kiri (%)|Pangkas dari atas (%)|Pangkas dari kanan (%)|Pangkas dari bawah (%)|Mata uang tanda terima',
}

NOTICES = {
 'es-ES es-419': {
  'sort':'Ordenar','status_donated':'Donado','fewer_details':'Menos detalles',
  'dashboard_subtitle':'Más claridad para todo lo que forma tu hogar.',
  'hero_body':'Tus pertenencias, recibos y garantías.\\nTodo reunido con cuidado.',
  'no_warranties_due':'No hay garantías que venzan pronto.',
  'reminder_days':'Avisar antes del vencimiento (días)',
  'form_error':'Comprueba el nombre, los importes no negativos y las fechas (YYYY-MM-DD). La fecha de fin de la garantía debe ser posterior a la de inicio.',
  'remove_attachment':'Eliminar archivo adjunto','maintenance_title':'Cuida lo que conservas.',
  'backup_body':'Guarda un ZIP completo con registros y archivos adjuntos. Al restaurar, se verifican las sumas de comprobación y se combinan los datos por ID; los registros existentes tienen prioridad. Guarda una copia antes de desinstalar.',
  'app_lock':'Exigir el desbloqueo del dispositivo',
 },
 'fr-FR fr-CA': {
  'add_household':'Ajouter un foyer','a_little_order':'Un peu d’ordre. Beaucoup de sérénité.',
  'hero_body':'Vos biens, reçus et garanties.\\nRéunis avec soin.',
  'skip':'Passer pour le moment','warranty_end':'Expiration de la garantie',
  'maintenance_title':'Prenez soin de vos biens.',
  'backup_body':'Enregistrez un ZIP complet avec les données et les pièces jointes. La restauration vérifie les sommes de contrôle et fusionne les données par identifiant ; les enregistrements existants sont conservés en cas de conflit. Gardez une sauvegarde avant de désinstaller.',
  'voice_notice':'Appuyez pour parler, puis vérifiez la transcription. La reconnaissance dépend de votre appareil et peut nécessiter Internet. Possessory ne conserve pas les enregistrements audio bruts. Utilisez les commandes prises en charge ci-dessous. Les commandes avancées nécessitent Premium.',
 },
 'de-DE': {
  'hero_body':'Ihre Gegenstände, Belege und Garantien.\\nSorgfältig zusammen aufbewahrt.',
  'skip':'Vorerst überspringen','reminder_days':'Vor Ablauf erinnern (Tage)',
  'form_error':'Prüfen Sie den Namen, nicht negative Beträge und Datumsangaben (YYYY-MM-DD). Das Garantieende muss nach dem Beginn liegen.',
  'remove_attachment':'Anhang entfernen',
  'voice_notice':'Drücken Sie zum Sprechen und prüfen Sie anschließend den erkannten Text. Die Verfügbarkeit hängt vom Gerät ab und kann Internet erfordern. Possessory speichert keine Audioaufnahmen. Verwenden Sie die unten aufgeführten Befehle. Erweiterte Befehle erfordern Premium.',
 },
 'it-IT': {
  'add_household':'Aggiungi abitazione','hero_body':'I tuoi beni, le ricevute e le garanzie.\\nConservati insieme con cura.',
  'form_error':'Controlla il nome, gli importi non negativi e le date (YYYY-MM-DD). La fine della garanzia deve essere successiva all’inizio.',
  'maintenance_title':'Prenditi cura di ciò che possiedi.',
  'maintenance_disclaimer':'I promemoria aiutano a organizzare la manutenzione e non sostituiscono le ispezioni di sicurezza professionali.',
  'backup_body':'Salva un archivio ZIP completo di dati e allegati. Il ripristino verifica i checksum e unisce i dati per ID; in caso di conflitto mantiene i dati esistenti. Conserva un backup prima di disinstallare.',
  'restore_preview_body':'%1$d abitazioni · %2$d beni · %3$d allegati\\n\\nGli ID esistenti vengono mantenuti. Si aggiungono nuovi dati senza eliminare quelli esistenti. Le password dei backup non possono essere recuperate.',
  'voice_notice':'Premi per parlare, poi controlla il testo riconosciuto. La disponibilità dipende dal dispositivo e può richiedere Internet. Possessory non conserva registrazioni audio. Usa i comandi supportati mostrati sotto. I comandi avanzati richiedono Premium.',
 },
 'pt-BR': {
  'hero_body':'Seus bens, recibos e garantias.\\nGuardados juntos com cuidado.',
  'add_room':'Adicionar cômodo','warranty_end':'Vencimento da garantia',
  'form_error':'Confira o nome, os valores não negativos e as datas (YYYY-MM-DD). O fim da garantia deve ser posterior ao início.',
  'maintenance_disclaimer':'Os lembretes ajudam a organizar a manutenção e não substituem inspeções de segurança profissionais.',
  'backup_body':'Salve um ZIP completo com registros e anexos. A restauração verifica as somas de verificação e combina os dados por ID; os registros existentes têm prioridade. Guarde um backup antes de desinstalar.',
  'voice_notice':'Pressione para falar e revise o texto reconhecido. A disponibilidade depende do dispositivo e pode exigir internet. Possessory não guarda gravações de áudio. Use os comandos compatíveis abaixo. Os comandos avançados exigem Premium.',
  'app_lock':'Exigir desbloqueio do dispositivo',
 },
 'pt-PT': {
  'hero_body':'Os seus bens, recibos e garantias.\\nGuardados juntos com cuidado.',
  'add_room':'Adicionar divisão',
  'backup_body':'Guarde um ZIP completo com registos e anexos. O restauro verifica as somas de verificação e combina os dados por ID; os registos existentes têm prioridade. Guarde uma cópia de segurança antes de desinstalar.',
  'voice_notice':'Prima para falar e reveja o texto reconhecido. A disponibilidade depende do dispositivo e pode exigir Internet. O Possessory não guarda gravações de áudio. Utilize os comandos suportados abaixo. Os comandos avançados exigem Premium.',
 },
 'nl-NL': {
  'hero_body':'Je spullen, bonnetjes en garanties.\\nZorgvuldig bij elkaar bewaard.',
  'condition':'Staat',
  'warranty_disclaimer':'Je voert de garantiedatums zelf in. Fabrieksgarantie en wettelijke consumentenrechten staan los van elkaar; de toepasselijke voorwaarden bepalen of je aanspraak kunt maken op garantie.',
  'backup_body':'Sla een volledig ZIP-bestand met gegevens en bijlagen op. Bij herstel worden controlesommen gecontroleerd en gegevens samengevoegd op record-ID; bestaande gegevens blijven behouden bij een conflict. Maak een back-up voordat je de app verwijdert.',
 },
 'pl-PL': {
  'hero_title':'Miejsce dla\\nwszystkiego, co posiadasz.',
  'hero_body':'Twoje rzeczy, paragony i gwarancje.\\nStarannie przechowywane razem.',
  'maintenance_disclaimer':'Przypomnienia pomagają planować konserwację i nie zastępują profesjonalnych kontroli bezpieczeństwa.',
  'backup_body':'Zapisz pełny plik ZIP z danymi i załącznikami. Przywracanie sprawdza sumy kontrolne i łączy dane według identyfikatora; istniejące dane mają pierwszeństwo. Zachowaj kopię przed odinstalowaniem aplikacji.',
  'app_lock':'Wymagaj odblokowania urządzenia',
 },
 'sv-SE': {
  'hero_body':'Dina föremål, kvitton och garantier.\\nOmsorgsfullt samlade på ett ställe.',
  'skip':'Hoppa över tills vidare','condition':'Skick',
  'form_error':'Kontrollera namn, belopp som inte är negativa och datum (YYYY-MM-DD). Garantins slutdatum måste ligga efter startdatumet.',
  'maintenance_title':'Ta hand om det du äger.',
  'voice_notice':'Tryck för att tala och kontrollera sedan den tolkade texten. Tillgängligheten beror på enheten och kan kräva internet. Possessory sparar inga ljudinspelningar. Använd kommandona nedan. Utökade kommandon kräver Premium.',
 },
 'tr-TR': {
  'status_donated':'Bağışlandı','add_household':'Ev ekle','sort':'Sırala','skip':'Şimdilik atla',
  'hero_title':'Sahip olduğunuz her şey\\niçin bir yer.',
  'hero_body':'Eşyalarınız, makbuzlarınız ve garantileriniz.\\nÖzenle bir arada.',
  'no_warranties_due':'Yakında süresi dolacak garanti yok.', 'warranty_end':'Garanti bitiş tarihi',
  'reminder_days':'Bitişten önce hatırlat (gün)',
  'form_error':'Adı, negatif olmayan tutarları ve tarihleri (YYYY-MM-DD) kontrol edin. Garanti bitişi başlangıcından sonra olmalıdır.',
  'remove_attachment':'Eki kaldır','maintenance_title':'Eşyalarınıza özen gösterin.',
  'maintenance_disclaimer':'Hatırlatmalar bakımı planlamaya yardımcı olur; profesyonel güvenlik kontrollerinin yerini tutmaz.',
  'backup_body':'Kayıtları ve ekleri içeren tam bir ZIP kaydedin. Geri yükleme sağlama toplamlarını doğrular ve kayıt kimliğine göre birleştirir; mevcut kayıtlar korunur. Uygulamayı kaldırmadan önce yedek alın.',
  'voice_notice':'Konuşmak için basın, ardından tanınan metni kontrol edin. Kullanılabilirlik cihazınıza bağlıdır ve internet gerektirebilir. Possessory ham ses kayıtlarını saklamaz. Aşağıdaki desteklenen komutları kullanın. Gelişmiş komutlar Premium gerektirir.',
  'app_lock':'Cihaz kilidinin açılmasını zorunlu tut',
 },
 'id-ID': {
  'hero_body':'Barang, tanda terima, dan garansi Anda.\\nTersimpan rapi bersama.',
  'no_warranties_due':'Tidak ada garansi yang segera berakhir.', 'warranty_end':'Tanggal berakhir garansi',
  'warranty_disclaimer':'Tanggal garansi dicatat oleh Anda. Garansi produsen dan hak konsumen berdasarkan hukum adalah hal terpisah; kelayakan bergantung pada ketentuan yang berlaku.',
  'form_error':'Periksa nama, jumlah yang tidak negatif, dan tanggal (YYYY-MM-DD). Akhir garansi harus setelah tanggal mulainya.',
  'maintenance_disclaimer':'Pengingat membantu mengatur perawatan dan tidak menggantikan pemeriksaan keselamatan profesional.',
  'backup_body':'Simpan ZIP lengkap dengan catatan dan lampiran. Pemulihan memeriksa checksum dan menggabungkan berdasarkan ID; catatan yang ada tetap dipertahankan jika terjadi konflik. Simpan cadangan sebelum menghapus aplikasi.',
 },
}

def main():
    path = ROOT/'localization/editorial-overrides.json'
    data = json.loads(path.read_text(encoding='utf-8'))
    for locales, row in ROWS.items():
        values = row.split('|')
        assert len(KEYS) == len(values)
        for locale in locales.split():
            data.setdefault(locale, {}).update(zip(KEYS, values))
            data[locale]['column_room'] = data[locale]['room']
    for locales, entries in NOTICES.items():
        for locale in locales.split(): data.setdefault(locale, {}).update(entries)
    corrections = {
        'es-ES': {'maintenance_due': 'Mantenimiento pendiente', 'archive': 'Archivar',
                  'unarchive': 'Reactivar', 'restore_backup': 'Abrir copia de seguridad',
                  'receipt_reference': 'Referencia del recibo'},
        'es-419': {'maintenance_due': 'Mantenimiento pendiente', 'archive': 'Archivar',
                  'unarchive': 'Reactivar', 'restore_backup': 'Abrir copia de seguridad',
                  'receipt_reference': 'Referencia del recibo'},
        'fr-FR': {'maintenance_due': 'Entretien à effectuer', 'unarchive': 'Réactiver',
                  'receipt_reference': 'Référence du reçu'},
        'fr-CA': {'maintenance_due': 'Entretien à effectuer', 'unarchive': 'Réactiver',
                  'receipt_reference': 'Référence du reçu'},
        'it-IT': {'maintenance_due': 'Manutenzione da eseguire', 'archive': 'Archivia',
                  'unarchive': 'Riattiva', 'receipt_reference': 'Riferimento della ricevuta'},
        'pt-BR': {'maintenance_due': 'Manutenção pendente', 'archive': 'Arquivar',
                  'restore_backup': 'Abrir backup'},
        'pt-PT': {'maintenance_due': 'Manutenção pendente', 'archive': 'Arquivar',
                  'unarchive': 'Reativar'},
        'nl-NL': {'maintenance_due': 'Gepland onderhoud', 'archive': 'Archiveren',
                  'with_receipts': 'Met bonnen', 'receipt_reference': 'Bonreferentie'},
        'pl-PL': {'maintenance_due': 'Konserwacja do wykonania', 'model': 'Model',
                  'archive': 'Archiwizuj', 'with_receipts': 'Z paragonami',
                  'receipt_reference': 'Numer paragonu'},
        'id-ID': {'maintenance_due': 'Perawatan yang jatuh tempo', 'archive': 'Arsipkan',
                  'unarchive': 'Aktifkan kembali', 'with_receipts': 'Dengan tanda terima',
                  'receipt_reference': 'Referensi tanda terima'},
        'de-DE': {'missing_receipts': 'Fehlende Belege', 'maintenance_due': 'Fällige Wartung',
                  'serial': 'Seriennummer', 'duplicate': 'Duplizieren', 'archive': 'Archivieren',
                  'unarchive': 'Wieder aktivieren', 'with_receipts': 'Mit Belegen',
                  'restore_backup': 'Sicherung öffnen', 'unlock': 'Entsperren',
                  'receipt_reference': 'Belegreferenz'},
        'sv-SE': {'art': 'Konst', 'condition_new': 'Nytt', 'condition_good': 'Bra',
                  'maintenance_due': 'Underhåll att utföra', 'brand': 'Varumärke', 'model': 'Modell',
                  'archive': 'Arkivera', 'restore_backup': 'Öppna säkerhetskopia',
                  'receipt_reference': 'Kvittoreferens'},
        'tr-TR': {'kitchen': 'Mutfak', 'bathroom': 'Banyo', 'home_office': 'Ev ofisi',
                  'garage': 'Garaj', 'storage': 'Depolama', 'electronics': 'Elektronik',
                  'category': 'Kategori', 'tags': 'Etiketler', 'archive': 'Arşivle',
                  'premium': 'Possessory Premium', 'column_category': 'Kategori',
                  'maintenance_due': 'Zamanı gelen bakım', 'unarchive': 'Yeniden etkinleştir',
                  'all_items': 'Tüm etkin eşyalar', 'with_receipts': 'Makbuzu olanlar',
                  'restore_backup': 'Yedek aç', 'receipt_reference': 'Makbuz referansı',
                  'warranty_manufacturer': 'Üretici'},
    }
    for locale, entries in corrections.items():
        data.setdefault(locale, {}).update(entries)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
    print('Recorded common-label corrections; no approval changed.')

if __name__ == '__main__': main()
