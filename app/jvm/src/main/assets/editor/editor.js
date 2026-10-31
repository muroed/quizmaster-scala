'use strict';
const $ = id => document.getElementById(id);
let revision = '';
async function request(url, options = {}) {
  options.headers = { 'X-Requested-With': 'QuizmasterEditor', ...(options.headers || {}) };
  const response = await fetch(url, options);
  const data = await response.json();
  if (!response.ok) throw new Error(data.error || 'Ошибка запроса');
  if (data.revision) revision = data.revision;
  return data;
}
function report(error) { $('status').textContent = error.message || String(error); }
function filename() {
  const name = $('name').value.trim();
  if (!/^[A-Za-z0-9][A-Za-z0-9_-]{0,63}\.ya?ml$/.test(name)) throw new Error('Имя файла: латинские буквы, цифры, дефис или подчёркивание и расширение .yml');
  return name;
}
async function refresh() {
  const data = await request('/editor/api/quizzes');
  $('quizzes').replaceChildren(...data.quizzes.map(name => new Option(name, name)));
}
$('open').onclick = async () => { try { const name = $('quizzes').value; if (!name) throw new Error('Нет сохранённых квизов'); const data = await request('/editor/api/quizzes/' + encodeURIComponent(name)); $('name').value = name; $('yaml').value = data.yaml; $('status').textContent = 'Квиз загружен'; } catch (e) { report(e); } };
$('new').onclick = () => { $('name').value = ''; $('yaml').value = 'title: Новый квиз\nlanguage: en\nrounds:\n  - name: Раунд 1\n    questions:\n      - question: Вопрос\n        answer: Ответ\n'; $('status').textContent = 'Введите имя файла и сохраните квиз'; };
$('save').onclick = async () => { try { const name = filename(); await request('/editor/api/quizzes/' + encodeURIComponent(name), {method:'PUT', headers:{'Content-Type':'application/json'}, body:JSON.stringify({yaml:$('yaml').value, revision})}); await refresh(); $('quizzes').value = name; $('status').textContent = 'Квиз сохранён в GitHub'; } catch (e) { report(e); } };
$('upload').onclick = async () => { try { const file = $('media').files[0]; if (!file) throw new Error('Выберите файл'); const result = await request('/editor/api/media/' + encodeURIComponent(file.name), {method:'POST', headers:{'Content-Type':'application/octet-stream','X-Quiz-Revision':revision}, body:file}); $('uploaded').textContent = result.path; $('status').textContent = 'Файл загружен в GitHub'; } catch (e) { report(e); } };
$('activate').onclick = async () => { try { const name = filename(); if (!confirm('Запустить этот квиз? Текущая игра и её результаты будут сброшены.')) return; const result = await request('/editor/api/activate/' + encodeURIComponent(name), {method:'POST', headers:{'X-Quiz-Revision':revision}}); $('status').textContent = result.message; } catch (e) { report(e); } };
refresh().then(() => { if ($('quizzes').value) $('open').click(); }).catch(report);
